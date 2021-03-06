package dev.momostudios.coldsweat.util.entity;

import dev.momostudios.coldsweat.api.registry.TempModifierRegistry;
import net.minecraft.nbt.*;
import dev.momostudios.coldsweat.api.temperature.modifier.TempModifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class NBTHelper
{
    public static Object getObjectFromTag(Tag inbt)
    {
        if (inbt instanceof StringTag stringnbt)
        {
            return stringnbt.getAsString();
        }
        else if (inbt instanceof IntTag intnbt)
        {
            return intnbt.getAsInt();
        }
        else if (inbt instanceof FloatTag floatnbt)
        {
            return floatnbt.getAsFloat();
        }
        else if (inbt instanceof DoubleTag dbnbt)
        {
            return dbnbt.getAsDouble();
        }
        else if (inbt instanceof ShortTag shortnbt)
        {
            return shortnbt.getAsShort();
        }
        else if (inbt instanceof LongTag longnbt)
        {
            return longnbt.getAsLong();
        }
        else if (inbt instanceof IntArrayTag int2nbt)
        {
            return int2nbt.getAsIntArray();
        }
        else if (inbt instanceof LongArrayTag long2nbt)
        {
            return long2nbt.getAsLongArray();
        }
        else if (inbt instanceof ByteArrayTag byte2nbt)
        {
            return byte2nbt.getAsByteArray();
        }
        else if (inbt instanceof ByteTag)
        {
            return ((ByteTag) inbt).getAsByte() != 0;
        }
        else throw new UnsupportedOperationException("Unsupported Tag type: " + inbt.getClass().getName());
    }

    public static Tag getTagFromObject(Object object)
    {
        if (object instanceof String str)
        {
            return StringTag.valueOf(str);
        }
        else if (object instanceof Integer intr)
        {
            return IntTag.valueOf(intr);
        }
        else if (object instanceof Float flt)
        {
            return FloatTag.valueOf(flt);
        }
        else if (object instanceof Double dbl)
        {
            return DoubleTag.valueOf(dbl);
        }
        else if (object instanceof Short shrt)
        {
            return ShortTag.valueOf(shrt);
        }
        else if (object instanceof Long lng)
        {
            return LongTag.valueOf(lng);
        }
        else if (object instanceof int[] intarr)
        {
            return new IntArrayTag(intarr);
        }
        else if (object instanceof long[] lngarr)
        {
            return new LongArrayTag(lngarr);
        }
        else if (object instanceof byte[] bytearr)
        {
            return new ByteArrayTag(bytearr);
        }
        else if (object instanceof Boolean bool)
        {
            return ByteTag.valueOf(bool ? (byte) 1 : (byte) 0);
        }
        else
        {
            throw new UnsupportedOperationException("Unsupported object type: " + object.getClass().getName());
        }
    }

    public static CompoundTag modifierToTag(TempModifier modifier)
    {
        // Write the modifier's data to a CompoundTag
        CompoundTag modifierTag = new CompoundTag();
        modifierTag.putString("id", modifier.getID());

        // Add the modifier's arguments
        modifier.getArguments().forEach((name, value) ->
        {
            modifierTag.put(name, getTagFromObject(value));
        });

        // Read the modifier's expiration time
        if (modifier.getExpireTime() != -1)
            modifierTag.putInt("expireTicks", modifier.getExpireTime());

        // Read the modifier's tick rate
        if (modifier.getTickRate() > 1)
            modifierTag.putInt("tickRate", modifier.getTickRate());

        // Read the modifier's ticks existed
        modifierTag.putInt("ticksExisted", modifier.getTicksExisted());

        return modifierTag;
    }

    public static TempModifier tagToModifier(CompoundTag modifierTag)
    {
        // Create a new modifier from the CompoundTag
        TempModifier newModifier = TempModifierRegistry.getEntryFor(modifierTag.getString("id"));

        if (newModifier == null) return null;
        modifierTag.getAllKeys().forEach(key ->
        {
            // Add the modifier's arguments
            List<String> invalidArgs = Arrays.asList("id", "expireTicks", "tickRate", "ticksExisted");
            if (key != null && !invalidArgs.contains(modifierTag.get(key).getAsString()))
            {
                newModifier.addArgument(key, getObjectFromTag(modifierTag.get(key)));
            }
        });

        // Set the modifier's expiration time
        if (modifierTag.contains("expireTicks"))
            newModifier.expires(modifierTag.getInt("expireTicks"));

        // Set the modifier's tick rate
        if (modifierTag.contains("tickRate"))
            newModifier.tickRate(modifierTag.getInt("tickRate"));

        // Set the modifier's ticks existed
        newModifier.setTicksExisted(modifierTag.getInt("ticksExisted"));

        return newModifier;
    }

    public static void incrementTag(Object owner, String key, int amount)
    {
        incrementTag(owner, key, amount, (tag) -> true);
    }

    public static int incrementTag(Object owner, String key, int amount, Predicate<Integer> predicate)
    {
        CompoundTag tag;
        if (owner instanceof LivingEntity entity)
        {
            tag = entity.getPersistentData();
        }
        else if (owner instanceof ItemStack stack)
        {
            tag = stack.getOrCreateTag();
        }
        else if (owner instanceof BlockEntity blockEntity)
        {
            tag = blockEntity.getTileData();
        }
        else return 0;

        int value = tag.getInt(key);
        if (predicate.test(value))
        {
            tag.putInt(key, value + amount);
        }
        return value + amount;
    }
}
