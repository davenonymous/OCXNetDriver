package org.dave.ocxnetdriver.driver.controller;


import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.prefab.DriverSidedBlock;
import mcjty.xnet.api.channels.IControllerContext;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class DriverXnetController extends DriverSidedBlock {

    @Override
    public boolean worksWith(World world, BlockPos pos, EnumFacing side) {
        if(!world.getBlockState(pos).getBlock().getRegistryName().toString().equals("xnet:controller")) {
            return false;
        }

        TileEntity te = world.getTileEntity(pos);
        if(!(te instanceof IControllerContext)) {
            return false;
        }

        return true;
    }

    @Override
    public ManagedEnvironment createEnvironment(World world, BlockPos pos, EnumFacing side) {
        return new EnvironmentXnetController((IControllerContext) world.getTileEntity(pos));
    }
}
