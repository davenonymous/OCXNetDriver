package org.dave.ocxnetdriver.driver.controller;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.NamedBlock;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import mcjty.xnet.api.channels.IControllerContext;
import mcjty.xnet.api.keys.SidedPos;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import org.dave.ocxnetdriver.config.ConfigurationHandler;
import org.dave.ocxnetdriver.converter.ConverterBlockPos;
import org.dave.ocxnetdriver.util.CachedReflectionHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnvironmentXnetController extends AbstractManagedEnvironment implements NamedBlock {
    protected final IControllerContext controller;

    public EnvironmentXnetController(IControllerContext tileEntity) {
        controller = tileEntity;

        this.setNode(Network.newNode(this, Visibility.Network).withComponent("xnet", Visibility.Network).create());
    }

    @Callback(doc = "function(sourcePos:table, amount:number, targetPos:table[, sourceSide:number[, targetSide:number]]):number -- Transfer energy between two energy handlers")
    public Object[] transferEnergy(final Context context, final Arguments args) {
        BlockPos pos = ConverterBlockPos.checkBlockPos(args, 0);
        SidedPos sidedPos = controller.getConnectedBlockPositions().stream()
                .filter(sp -> sp.getPos().equals(pos)).findFirst()
                .orElse(null);

        if(sidedPos == null) {
            return new Object[]{ null, "given source position is not connected to the network" };
        }

        int amount = args.checkInteger(1);

        BlockPos targetPos = ConverterBlockPos.checkBlockPos(args, 2);
        SidedPos targetSidedPos = controller.getConnectedBlockPositions().stream()
                .filter(sp -> sp.getPos().equals(targetPos)).findFirst()
                .orElse(null);

        if(targetSidedPos == null) {
            return new Object[]{ null, "given target position is not connected to the network" };
        }

        EnumFacing side = EnumFacing.getFront(args.optInteger(3, sidedPos.getSide().getIndex()));
        EnumFacing targetSide = EnumFacing.getFront(args.optInteger(4, targetSidedPos.getSide().getIndex()));


        World world = this.controller.getControllerWorld();
        TileEntity tileEntity = world.getTileEntity(pos);
        if(tileEntity == null) {
            return new Object[]{ null, "source is not a tile entity" };
        }

        if(!tileEntity.hasCapability(CapabilityEnergy.ENERGY, side)) {
            return new Object[]{ null, "source is no forge energy handler" };
        }

        TileEntity targetTileEntity = world.getTileEntity(targetPos);
        if(targetTileEntity == null) {
            return new Object[]{ null, "target is not a tile entity" };
        }

        if(!targetTileEntity.hasCapability(CapabilityEnergy.ENERGY, targetSide)) {
            return new Object[]{ null, "target is no forge energy handler" };
        }

        IEnergyStorage handler = tileEntity.getCapability(CapabilityEnergy.ENERGY, side);
        IEnergyStorage targetHandler = targetTileEntity.getCapability(CapabilityEnergy.ENERGY, targetSide);

        if(!handler.canExtract()) {
            return new Object[]{ null, "can not extract energy from source" };
        }

        if(!targetHandler.canReceive()) {
            return new Object[]{ null, "can not insert energy into target" };
        }

        int transferred = 0;
        int simulatedTicks = 0;
        int maxTicksToSimulate = ConfigurationHandler.Settings.ignoreEnergyTransferLimits() ? ConfigurationHandler.Settings.getMaxEnergyTransferTicksPerCall() : 1;
        int lastTransfer = Integer.MAX_VALUE;
        List<String> errors = new ArrayList<>();
        while(transferred < amount && lastTransfer > 0 && simulatedTicks < maxTicksToSimulate) {
            int simAmount = handler.extractEnergy(amount - transferred, true);
            if(simAmount <= 0) {
                errors.add("extractable amount from source is 0");
                break;
            }

            int simReceived = targetHandler.receiveEnergy(simAmount, true);
            if(simReceived <= 0) {
                errors.add("insertable amount into target is 0");
                break;
            }

            simulatedTicks++;
            handler.extractEnergy(simReceived, false);
            targetHandler.receiveEnergy(simReceived, false);

            transferred += simReceived;
        }

        return new Object[]{ transferred, errors };
    }

    @Callback(doc = "function(pos:table[, side: number]):table -- Get capacity and stored energy of the given energy handler")
    public Object[] getEnergy(final Context context, final Arguments args) {
        BlockPos pos = ConverterBlockPos.checkBlockPos(args, 0);
        SidedPos sidedPos = controller.getConnectedBlockPositions().stream()
                .filter(sp -> sp.getPos().equals(pos)).findFirst()
                .orElse(null);

        if(sidedPos == null) {
            return new Object[]{ null, "given position is not connected to the network" };
        }

        EnumFacing side = EnumFacing.getFront(args.optInteger(1, sidedPos.getSide().getIndex()));

        World world = this.controller.getControllerWorld();
        TileEntity tileEntity = world.getTileEntity(pos);
        if(tileEntity == null) {
            return new Object[]{ null, "not a tile entity" };
        }

        if(!tileEntity.hasCapability(CapabilityEnergy.ENERGY, side)) {
            return new Object[]{ null, "not a forge energy handler" };
        }

        IEnergyStorage handler = tileEntity.getCapability(CapabilityEnergy.ENERGY, side);

        HashMap<String, Object> result = new HashMap<>();
        result.put("capacity", handler.getMaxEnergyStored());
        result.put("stored", handler.getEnergyStored());
        result.put("canExtract", handler.canExtract());
        result.put("canReceive", handler.canReceive());

        return new Object[]{ result };
    }

    @Callback(doc = "function(sourcePos:table, amount:number, targetPos:table[, sourceSide:number[, targetSide:number]]):number -- Transfer fluids between two tanks")
    public Object[] transferFluid(final Context context, final Arguments args) {
        BlockPos pos = ConverterBlockPos.checkBlockPos(args, 0);
        SidedPos sidedPos = controller.getConnectedBlockPositions().stream()
                .filter(sp -> sp.getPos().equals(pos)).findFirst()
                .orElse(null);

        if(sidedPos == null) {
            return new Object[]{ null, "given source position is not connected to the network" };
        }

        int amount = args.checkInteger(1);

        BlockPos targetPos = ConverterBlockPos.checkBlockPos(args, 2);
        SidedPos targetSidedPos = controller.getConnectedBlockPositions().stream()
                .filter(sp -> sp.getPos().equals(targetPos)).findFirst()
                .orElse(null);

        if(targetSidedPos == null) {
            return new Object[]{ null, "given target position is not connected to the network" };
        }

        EnumFacing side = EnumFacing.getFront(args.optInteger(3, sidedPos.getSide().getIndex()));
        EnumFacing targetSide = EnumFacing.getFront(args.optInteger(4, targetSidedPos.getSide().getIndex()));


        World world = this.controller.getControllerWorld();
        TileEntity tileEntity = world.getTileEntity(pos);
        if(tileEntity == null) {
            return new Object[]{ null, "source is not a tile entity" };
        }

        if(!tileEntity.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side)) {
            return new Object[]{ null, "source is not an fluid handler" };
        }

        TileEntity targetTileEntity = world.getTileEntity(targetPos);
        if(targetTileEntity == null) {
            return new Object[]{ null, "target is not a tile entity" };
        }

        if(!targetTileEntity.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, targetSide)) {
            return new Object[]{ null, "target is not an fluid handler" };
        }

        IFluidHandler handler = tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);
        IFluidHandler targetHandler = targetTileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, targetSide);

        FluidStack simStack = handler.drain(amount, false);
        if(simStack == null) {
            return new Object[]{ null, "can not drain from source tank" };
        }

        int simAmount = targetHandler.fill(simStack, false);
        if(simAmount <= 0) {
            return new Object[]{ null, "can not fill target tank" };
        }

        FluidStack realStack = handler.drain(simAmount, true);
        targetHandler.fill(realStack, true);

        return new Object[]{ simAmount };
    }

    @Callback(doc = "function(pos:table[, side: number]):table -- List all fluids in the given tank")
    public Object[] getFluids(final Context context, final Arguments args) {
        BlockPos pos = ConverterBlockPos.checkBlockPos(args, 0);
        SidedPos sidedPos = controller.getConnectedBlockPositions().stream()
                .filter(sp -> sp.getPos().equals(pos)).findFirst()
                .orElse(null);

        if(sidedPos == null) {
            return new Object[]{ null, "given position is not connected to the network" };
        }

        EnumFacing side = EnumFacing.getFront(args.optInteger(1, sidedPos.getSide().getIndex()));

        World world = this.controller.getControllerWorld();
        TileEntity tileEntity = world.getTileEntity(pos);
        if(tileEntity == null) {
            return new Object[]{ null, "not a tile entity" };
        }

        if(!tileEntity.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side)) {
            return new Object[]{ null, "not a fluid handler" };
        }

        IFluidHandler handler = tileEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side);

        List<Map<String, Object>> result = new ArrayList<>();
        for(IFluidTankProperties tank : handler.getTankProperties()) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("capacity", tank.getCapacity());
            map.put("content", tank.getContents());
            result.add(map);
        }

        return new Object[]{ result };
    }

    @Callback(doc = "function(sourcePos:table, sourceSlot:number, amount:number, targetPos:table[, sourceSide:number[, targetSide:number]]):number -- Transfer items between two inventories")
    public Object[] transferItem(final Context context, final Arguments args) {
        BlockPos pos = ConverterBlockPos.checkBlockPos(args, 0);
        SidedPos sidedPos = controller.getConnectedBlockPositions().stream()
                .filter(sp -> sp.getPos().equals(pos)).findFirst()
                .orElse(null);

        if(sidedPos == null) {
            return new Object[]{ null, "given source position is not connected to the network" };
        }

        int slot = args.checkInteger(1);
        int amount = args.checkInteger(2);


        BlockPos targetPos = ConverterBlockPos.checkBlockPos(args, 3);
        SidedPos targetSidedPos = controller.getConnectedBlockPositions().stream()
                .filter(sp -> sp.getPos().equals(targetPos)).findFirst()
                .orElse(null);

        if(targetSidedPos == null) {
            return new Object[]{ null, "given target position is not connected to the network" };
        }

        EnumFacing side = EnumFacing.getFront(args.optInteger(4, sidedPos.getSide().getIndex()));
        EnumFacing targetSide = EnumFacing.getFront(args.optInteger(5, targetSidedPos.getSide().getIndex()));


        World world = this.controller.getControllerWorld();
        TileEntity tileEntity = world.getTileEntity(pos);
        if(tileEntity == null) {
            return new Object[]{ null, "source is not a tile entity" };
        }

        if(!tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) {
            return new Object[]{ null, "source is not an item handler" };
        }

        TileEntity targetTileEntity = world.getTileEntity(targetPos);
        if(targetTileEntity == null) {
            return new Object[]{ null, "target is not a tile entity" };
        }

        if(!targetTileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, targetSide)) {
            return new Object[]{ null, "target is not an item handler" };
        }

        IItemHandler handler = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
        IItemHandler targetHandler = targetTileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, targetSide);

        ItemStack sourceStackSim = handler.extractItem(slot-1, amount, true);
        if(sourceStackSim == null || sourceStackSim.isEmpty()) {
            return new Object[]{ null, "can not extract from source slot" };
        }

        ItemStack returnStackSim = ItemHandlerHelper.insertItemStacked(targetHandler, sourceStackSim, true);

        int transferrableAmount = amount;
        if(returnStackSim != null && !returnStackSim.isEmpty()) {
            transferrableAmount = sourceStackSim.getCount() - returnStackSim.getCount();
        }

        if(transferrableAmount > 0) {
            ItemStack sourceStackReal = handler.extractItem(slot-1, transferrableAmount, false);
            ItemHandlerHelper.insertItemStacked(targetHandler, sourceStackReal, false);

            return new Object[]{ transferrableAmount };
        }
        return new Object[]{ null, "can not insert into target" };
    }

    @Callback(doc = "function(pos:table[, side: number]):table -- List all items in the given inventory")
    public Object[] getItems(final Context context, final Arguments args) {
        BlockPos pos = ConverterBlockPos.checkBlockPos(args, 0);
        SidedPos sidedPos = controller.getConnectedBlockPositions().stream()
                .filter(sp -> sp.getPos().equals(pos)).findFirst()
                .orElse(null);

        if(sidedPos == null) {
            return new Object[]{ null, "given position is not connected to the network" };
        }

        EnumFacing side = EnumFacing.getFront(args.optInteger(1, sidedPos.getSide().getIndex()));

        World world = this.controller.getControllerWorld();
        TileEntity tileEntity = world.getTileEntity(pos);
        if(tileEntity == null) {
            return new Object[]{ null, "not a tile entity" };
        }

        if(!tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) {
            return new Object[]{ null, "not an item handler" };
        }

        IItemHandler handler = tileEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side);
        List<ItemStack> result = new ArrayList<>();
        for(int slot = 0; slot < handler.getSlots(); slot++) {
            result.add(handler.getStackInSlot(slot));
        }
        return new Object[]{ result };
    }

    @Callback(doc = "function(pos:table[, side: number]):table -- List all capabilities of the given block at the given or connected side")
    public Object[] getSupportedCapabilities(final Context context, final Arguments args) {
        BlockPos pos = ConverterBlockPos.checkBlockPos(args, 0);

        SidedPos sidedPos = controller.getConnectedBlockPositions().stream()
                .filter(sp -> sp.getPos().equals(pos)).findFirst()
                .orElse(null);

        if(sidedPos == null) {
            return new Object[]{ null, "given position is not connected to the network" };
        }

        EnumFacing side = EnumFacing.getFront(args.optInteger(1, sidedPos.getSide().getIndex()));

        World world = this.controller.getControllerWorld();

        TileEntity tileEntity = world.getTileEntity(pos);
        if(tileEntity == null) {
            return new Object[]{ null, "not a tile entity" };
        }

        List<String> result = new ArrayList<>();
        if(tileEntity.hasCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side)) {
            result.add("items");
        }
        if(tileEntity.hasCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, side)) {
            result.add("fluid");
        }
        if(tileEntity.hasCapability(CapabilityEnergy.ENERGY, side)) {
            result.add("energy");
        }

        return new Object[]{result};
    }

    @Callback(doc = "function():table -- List all blocks connected to the XNet network")
    public Object[] getConnectedBlocks(final Context context, final Arguments args) {
        List<Map<String, Object>> result = new ArrayList<>();
        for(SidedPos pos : controller.getConnectedBlockPositions()) {
            HashMap<String, Object> map = new HashMap<>();
            IBlockState state = controller.getControllerWorld().getBlockState(pos.getPos());

            map.put("pos", pos.getPos());
            map.put("side", pos.getSide());
            map.put("name", state.getBlock().getRegistryName());
            map.put("meta", state.getBlock().getMetaFromState(state));

            BlockPos connectorPos = pos.getPos().offset(pos.getSide());

            String registryName = controller.getControllerWorld().getBlockState(connectorPos).getBlock().getRegistryName().toString();
            if(registryName.equals("xnet:advanced_connector") || registryName.equals("xnet:connector")) {
                String connectorName = CachedReflectionHelper.getFieldValue(String.class, controller.getControllerWorld().getTileEntity(connectorPos), "name");
                if(connectorName != null && connectorName.length() > 0) {
                    map.put("connector", connectorName);
                }
            }

            result.add(map);
        }

        return new Object[] { result };
    }

    @Override
    public String preferredName() {
        return "xnet";
    }

    @Override
    public int priority() {
        return 1;
    }
}
