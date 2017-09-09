package org.dave.ocxnetdriver.converter;

import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.machine.Arguments;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public class ConverterBlockPos implements Converter {

    @Override
    public void convert(Object value, Map<Object, Object> output) {
        if(value instanceof BlockPos) {
            BlockPos pos = (BlockPos) value;
            output.put("x", pos.getX());
            output.put("y", pos.getY());
            output.put("z", pos.getZ());
        }
    }

    public static BlockPos checkBlockPos(Arguments args, int index) {
        Map<String, Object> map = args.checkTable(index);
        if(!map.containsKey("x") || !(map.get("x") instanceof Double)) {
            throw new IllegalArgumentException("Missing x value in table");
        }
        if(!map.containsKey("y") || !(map.get("y") instanceof Double)) {
            throw new IllegalArgumentException("Missing y value in table");
        }
        if(!map.containsKey("z") || !(map.get("z") instanceof Double)) {
            throw new IllegalArgumentException("Missing z value in table");
        }

        return new BlockPos((double)map.get("x"), (double)map.get("y"), (double)map.get("z"));
    }
}
