package com.neep.neepmeat.client.sound;

import com.jozufozu.flywheel.util.AnimationTickHolder;
import com.neep.neepmeat.BalanceConstants;
import com.neep.neepmeat.machine.pylon.PylonBlockEntity;
import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.Sound;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public class PylonSoundInstance extends AbstractSoundInstance implements TickableSoundInstance
{
    protected final Identifier soundId;
    private boolean done;
    private final PylonBlockEntity blockEntity;

    public PylonSoundInstance(PylonBlockEntity be, BlockPos pos, SoundEvent sound, SoundCategory category)
    {
        super(sound, category, Random.create());
        this.blockEntity = be;

        this.repeat = true;
        this.repeatDelay = 0;
        this.volume = 1.0f;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        this.soundId = sound.getId();
//        this.runningId = sound.getId();
    }

//    public boolean isRunning()
//    {
//        return blockEntity.getSpeed() >= PylonBlockEntity.RUNNING_SPEED;
//    }

    @Override
    public Identifier getId()
    {
        return this.soundId;
    }

    @Override
    public Sound getSound()
    {
        return sound;
    }

    @Override
    public boolean isDone()
    {
        return done;
    }

    @Override
    public float getVolume()
    {
        return this.volume * getSound().getVolume().get(random);
    }

    @Override
    public float getPitch()
    {
        return this.pitch * getSound().getPitch().get(random);
    }

    @Override
    public boolean canPlay()
    {
        return true;
    }

    @Override
    public boolean shouldAlwaysPlay()
    {
        return true;
    }

    @Override
    public void tick()
    {
        this.pitch = MathHelper.lerp(0.2f, this.pitch, MathHelper.lerp(MathHelper.clamp(blockEntity.getSpeed(), 0, BalanceConstants.PYLON_RUNNING_SPEED) / BalanceConstants.PYLON_RUNNING_SPEED, 0, 1));
        this.volume = pitch;

        if (blockEntity.isUnstable())
        {
            this.pitch += 0.6 * MathHelper.sin(AnimationTickHolder.getRenderTime());
        }

        if (this.blockEntity.isRemoved())
        {
            this.done = true;
            return;
        }

    }
}
