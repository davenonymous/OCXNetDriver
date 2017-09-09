# ocxnetdriver
OpenComputers + XNet = :heart:

Allows OpenComputers to utilize XNet networks to transfer items, fluids and energy to connected blocks.

## Known Issues

- Requires a patched version of XNet, see https://github.com/McJty/XNet/pull/157
- This is currently unrestricted, i.e. you can transfer humongous amounts in no time.

## Available Methods

| Method                                      | Description                                                      |
| ------------------------------------------- | ---------------------------------------------------------------- |
| getConnectedBlocks()                        | Returns a table with all blocks connected to the XNet network    |
| getSupportedCapabilities(pos[, side])       | List all capabilities the given block supports                   |
| getItems(pos[, side])                       | Returns a table with all items in the given inventory            |
| getFluids(pos[, side])                      | List all fluids in the given tank                                |
| getEnergy(pos[, side])                      | Get capacity and stored energy of the given energy handler       |
| transferItem(sourcePos, sourceSlot, amount, targetPos[, sourceSide[, targetSide]]) | Transfer items between two inventories      |
| transferFluid(sourcePos, amount, targetPos[, sourceSide[, targetSide]])            | Transfer fluids between two tanks           |
| transferEnergy(sourcePos, amount, targetPos[, sourceSide[, targetSide]])           | Transfer energy between two energy handlers |


## Synopsis

```lua
local component = require('component')
local sides = require('sides')

local xnet = component.xnet

local chests = {}
for i,block in ipairs(xnet.getConnectedBlocks()) do
    if(block.name == "minecraft:chest") then
        chests[#chests+1] = block
    end
end

-- Transfer 5 of whatever is in slot 1 of chest 1 into chest 2
print("Transferred: " .. tostring(xnet.transferItem(chests[1].pos, 1, 5, chests[2].pos)))
```