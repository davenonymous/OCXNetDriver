# ocxnetdriver
OpenComputers + XNet = :heart:

Allows OpenComputers to utilize XNet networks to transfer items, fluids and energy to connected blocks.

## Known Issues

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
| setDatabaseSlot(sourcePos, sourceSlot, database, entry[, sourceSide])              | Store an itemstack from somewhere in the XNet network in a database upgrade |

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

### Simple energy transfer example

```lua
local component = require('component')
local sides = require('sides')
local serialization = require('serialization')

local xnet = component.xnet

print(serialization.serialize(xnet.getConnectedBlocks()))
-- {
--    {pos={y=-1,x=0,z=0},name="thermalexpansion:cell",side="west",meta=0},
--    {pos={y=-1,x=1,z=0},name="opencomputers:case3",side="north",meta=1},
--    {pos={y=-1,x=-3,z=0},name="thermalexpansion:dynamo",side="east",meta=2,connector="Dynamo"},
--    {pos={y=0,x=0,z=0},name="xnet:controller",side="west",meta=3},
--    n=4
-- }

-- Find a dynamo and a cell and store their position etc
local energySource = nil
local energyTarget = nil

for i,block in ipairs(xnet.getConnectedBlocks()) do
    if(block.name == "thermalexpansion:dynamo") then
        energySource = block
    elseif(block.name == "thermalexpansion:cell") then
        energyTarget = block
    end
end

-- Abort if no source could be found
if(energySource == nil) then
    print("No dynamo found. Aborting!")
    os.exit(1)
end

-- Abort if no target could be found
if(energyTarget == nil) then
    print("No cell found. Aborting!")
    os.exit(2)
end

-- Transfer 1000RF from source to target using the sides next to the XNet Connector
local transferred, error = xnet.transferEnergy(energySource.pos, 1000, energyTarget.pos, sides[energySource.side], sides[energyTarget.side])
if(transferred ~= nil) then
    print(" - Transferred " .. transferred .. "RF to " .. serialization.serialize(energyTarget.pos))
    -- - Transferred 1000RF to {y=-1,x=0,z=0}
else
    print("Error: " .. error)
end
```