package com.neep.neepmeat.machine.large_motor;

import com.neep.meatlib.blockentity.SyncableBlockEntity;
import com.neep.neepmeat.BalanceConstants;
import com.neep.neepmeat.api.machine.MotorisedBlock;
import com.neep.neepmeat.api.processing.PowerUtils;
import com.neep.neepmeat.machine.motor.MotorEntity;
import com.neep.neepmeat.transport.api.pipe.AbstractBloodAcceptor;
import com.neep.neepmeat.transport.api.pipe.BloodAcceptor;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.jetbrains.annotations.Nullable;

public class LargeMotorBlockEntity extends SyncableBlockEntity implements MotorEntity
{
    public float angle; // TODO: thread things since this is modified concurrently in LargeMotorInstance
    public float currentSpeed;

    protected float influx;

    protected final float maxInflux = (float) BalanceConstants.LARGE_MOTOR_MAX_POWER / PowerUtils.referencePower();

    protected AbstractBloodAcceptor acceptor = new AbstractBloodAcceptor()
    {
        @Override
        public Mode getMode()
        {
            return Mode.SINK;
        }

        @Override
        public float updateInflux(float influx)
        {
            LargeMotorBlockEntity.this.influx = Math.min(influx, maxInflux);
            onPowerChange();
            return influx;
        }
    };

    @Nullable
    protected BlockApiCache<MotorisedBlock, Void> cache = null;
    private float loadTorque;
    private MotorisedBlock lastMotorised;

    public LargeMotorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state)
    {
        super(type, pos, state);
    }

    public void serverTick()
    {
        if (cache == null)
        {
            Direction facing = getCachedState().get(LargeMotorBlock.FACING);
            cache = BlockApiCache.create(MotorisedBlock.LOOKUP, (ServerWorld) world, pos.offset(facing, 2).up());
        }

        MotorisedBlock motorised = cache.find(null);
        if (motorised != null)
        {
            if (motorised.getLoadTorque() != loadTorque)
            {
                loadTorque = motorised.getLoadTorque();
                sync();
            }

            if (motorised != lastMotorised)
            {
                lastMotorised = motorised;
                onPowerChange();
            }

            motorised.motorTick(this);
        }
        else
        {
            lastMotorised = null;
        }
    }

    protected void onPowerChange()
    {
        MotorisedBlock motorised = getConnectedBlock();
        if (motorised != null)
        {
            // TODO: Decide on float or double
            motorised.setInputPower((float) this.getMechPUPower());
        }
        sync();
    }

    @Override
    public float getRotorAngle()
    {
        return angle;
    }

    public float getSpeed()
    {
        double power = PowerUtils.perUnitToAbsWatt(getMechPUPower());
        return (float) (power / Math.max(loadTorque, PowerUtils.MOTOR_TORQUE_LOSS));
    }

    @Override
    public double getMechPUPower()
    {
        return influx;
    }


    @Override
    public void writeNbt(NbtCompound nbt)
    {
        super.writeNbt(nbt);
        nbt.putFloat("loadTorque", loadTorque);
        nbt.putFloat("influx", influx);
    }

    @Override
    public void readNbt(NbtCompound nbt)
    {
        super.readNbt(nbt);
        this.loadTorque = nbt.getFloat("loadTorque");
        this.influx = nbt.getFloat("influx");
    }

    @Override
    public MotorisedBlock getConnectedBlock()
    {
        return cache != null ? cache.find(null) : null;
    }

    public BloodAcceptor getAcceptor(Direction face)
    {
        return acceptor;
    }
}
