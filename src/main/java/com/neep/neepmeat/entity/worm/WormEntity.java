package com.neep.neepmeat.entity.worm;

import com.neep.meatlib.api.entity.MultiPartEntity;
import com.neep.neepmeat.util.Bezier;
import com.neep.neepmeat.util.Easing;
import com.neep.neepmeat.util.NMMaths;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.GoalSelector;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec2f;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.ArrayList;
import java.util.List;

import static com.neep.neepmeat.network.NMTrackedData.DOUBLE;

public class WormEntity extends AbstractWormPart implements MultiPartEntity<WormEntity.WormSegment>, GeoEntity
{
    protected static final TrackedData<String> CURRENT_ACTION = DataTracker.registerData(WormEntity.class, TrackedDataHandlerRegistry.STRING);
    protected static final TrackedData<Double> HEAD_X = DataTracker.registerData(WormEntity.class, DOUBLE);
    protected static final TrackedData<Double> HEAD_Y = DataTracker.registerData(WormEntity.class, DOUBLE);
    protected static final TrackedData<Double> HEAD_Z = DataTracker.registerData(WormEntity.class, DOUBLE);
    protected static final TrackedData<Float> HEAD_PITCH = DataTracker.registerData(WormEntity.class, TrackedDataHandlerRegistry.FLOAT);
    protected static final TrackedData<Float> HEAD_YAW = DataTracker.registerData(WormEntity.class, TrackedDataHandlerRegistry.FLOAT);

    private final AnimatableInstanceCache instanceCache = GeckoLibUtil.createInstanceCache(this);
    private WormAction currentAction;
    protected List<WormSegment> segments = new ArrayList<>(17);
    protected List<WormSegment> tail = new ArrayList<>(16);
    public final WormSegment head;

    @Nullable protected LivingEntity target;

    protected final GoalSelector goalSelector;
//    protected final GoalSelector targetGoalSelector;

    public WormEntity(EntityType<? extends WormEntity> type, World world)
    {
        super(type, world);
        int length = 16;
        for (int i = 0; i < length; ++i)
        {
            WormSegment segment = new WormSegment(this, 26 / 16f, 16);
            tail.add(segment);
        }
        head = new WormSegment(this, 26 / 16f, 16);
        segments.addAll(tail);
        segments.add(head);

        this.goalSelector = new GoalSelector(getWorld().getProfilerSupplier());
//        this.targetGoalSelector = new GoalSelector(getWorld().getProfilerSupplier());

        if (!getWorld().isClient())
        {
            initGoals();
        }
    }

    protected void initGoals()
    {
        this.goalSelector.add(1, new WormTargetGoal(this, TargetPredicate.DEFAULT, 14));
        this.goalSelector.add(2, new WormFullSwingGoal(this, 8));
        this.goalSelector.add(2, new WormBiteGoal(this));
    }


    public void setTarget(LivingEntity target)
    {
        this.target = target;
    }

    public LivingEntity getTarget()
    {
        return target;
    }

    @Override
    protected void initDataTracker()
    {
        super.initDataTracker();
        dataTracker.startTracking(CURRENT_ACTION, WormAction.EmptyAction.ID.toString());

        dataTracker.startTracking(HEAD_X, getX());
        dataTracker.startTracking(HEAD_Y, getY() + 15);
        dataTracker.startTracking(HEAD_Z, getZ());
        dataTracker.startTracking(HEAD_PITCH, -90f);
        dataTracker.startTracking(HEAD_YAW, 0f);
    }

    public static DefaultAttributeContainer.Builder createLivingAttributes()
    {
        return MobEntity.createMobAttributes();
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt)
    {
        super.writeNbt(nbt);

        nbt.put("currentAction", WormActions.toNbt(currentAction));
        return nbt;
    }

    @Override
    public void readNbt(NbtCompound nbt)
    {
        super.readNbt(nbt);

        currentAction = WormActions.fromNbt(nbt.getCompound("currentAction"), this);
    }

    @Override
    public Packet<ClientPlayPacketListener> createSpawnPacket()
    {
        return new EntitySpawnS2CPacket(this);
    }

    @Override
    public void tick()
    {
        super.tick();
        Bezier.Cubic3 bezier = new Bezier.Cubic3(10);
        setPitch(-90);

        double y = getY();
        double x = getX();
        double z = getZ();

        if (getWorld().isClient())
        {
            updateHead();
        }

        Vec3d headLook = head.getPos().add(Vec3d.fromPolar(head.getPitch(), head.getYaw()).multiply(-8));

        bezier.setPoint(0, x, y, z);
        bezier.setPoint(1, x, y + 5, z);
        bezier.setPoint(2, headLook.x, headLook.y, headLook.z);
        bezier.setPoint(3, head.getX(), head.getY(), head.getZ());

        for (int i = 0; i < tail.size(); ++i)
        {
            WormSegment segment = tail.get(i);
            float delta = ((float) i) / tail.size();

            double l = bezier.length();
            double t = bezier.tForDistance(delta * bezier.length());
            Vec3d X = bezier.value(t);
            Vec3d U = bezier.derivative(t);

//            double x1 = Bezier.bezier3(delta, x, x, headLook.x, head.getX());
//            double y1 = Bezier.bezier3(delta, y, y + 5, headLook.y, head.getY());
//            double z1 = Bezier.bezier3(delta, z, z, headLook.z, head.getZ());
//
//            double u = Bezier.derivative3(delta, x, x, headLook.x, head.getX());
//            double v = Bezier.derivative3(delta, y, y + 5, headLook.y, head.getY());
//            double w = Bezier.derivative3(delta, z, z, headLook.z, head.getZ());

            Vec2f pitchYaw = NMMaths.rectToPol(U.x, U.y, U.z);

            segment.updatePositionAndAngles(X.x, X.y, X.z, pitchYaw.y, pitchYaw.x);
        }

        updateGoalControls();
    }

    @Override
    protected void tickNewAi()
    {
        int i = this.getWorld().getServer().getTicks() + this.getId();
        if (i % 2 == 0 || this.age <= 1)
        {
//            this.getWorld().getProfiler().push("targetSelector");
//            this.targetSelector.tick();
//            this.getWorld().getProfiler().pop();
            this.getWorld().getProfiler().push("goalSelector");
            this.goalSelector.tick();
            this.getWorld().getProfiler().pop();
        }
        else
        {
//            this.getWorld().getProfiler().push("targetSelector");
//            this.targetSelector.tickGoals(false);
//            this.getWorld().getProfiler().pop();
            this.getWorld().getProfiler().push("goalSelector");
            this.goalSelector.tickGoals(false);
            this.getWorld().getProfiler().pop();
        }
    }

    protected void updateGoalControls()
    {
        this.goalSelector.setControlEnabled(Goal.Control.MOVE, true);
        this.goalSelector.setControlEnabled(Goal.Control.TARGET, true);
    }

    protected Vec3d getNeutralHeadPos()
    {
        return new Vec3d(getX(), getY() + 15, getZ());
    }

    private Vec2f getNeutralHeadPitch()
    {
        return new Vec2f(-90, 0);
    }

    protected void updateHead()
    {
//        double x = dataTracker.get(HEAD_X);
//        double y = dataTracker.get(HEAD_Y);
//        double z = dataTracker.get(HEAD_Z);
//        float pitch = dataTracker.get(HEAD_PITCH);
//        float yaw = dataTracker.get(HEAD_YAW);
        head.updatePositionAndAngles(dataTracker.get(HEAD_X), dataTracker.get(HEAD_Y), dataTracker.get(HEAD_Z),
                dataTracker.get(HEAD_YAW), dataTracker.get(HEAD_PITCH));
    }

    @Override
    public void remove(RemovalReason reason)
    {
        segments.forEach(s -> s.remove(reason));
        super.remove(reason);
    }

    protected WormActions.Entry chooseAction()
    {
        return WormActions.random();
    }

    public void setHeadPos(double x, double y, double z)
    {
        dataTracker.set(HEAD_X, x);
        dataTracker.set(HEAD_Y, y);
        dataTracker.set(HEAD_Z, z);
        head.setPos(x, y, z);
    }

    public void setHeadAngles(float pitch, float yaw)
    {
        if (!Float.isNaN(pitch))
        {
            head.setPitch(pitch);
            dataTracker.set(HEAD_PITCH, pitch);
        }
        if (!Float.isNaN(yaw))
        {
            head.setYaw(yaw);
            dataTracker.set(HEAD_YAW, yaw);
        }
    }

    protected PlayState controller(final AnimationState<WormEntity> event)
    {
//        String anim = WormActions.getAnimation(dataTracker.get(CURRENT_ACTION));
//        event.getController().setAnimation(WormActions.getAnimation(dataTracker.get(CURRENT_ACTION)));

        return PlayState.CONTINUE;
    }

    @Override
    public Iterable<WormSegment> getParts()
    {
        return segments;
    }

    public void lerpHeadPos(int tick, int totalTicks, Vec3d toPos)
    {
        float delta = (float) tick / totalTicks;
        double headX = MathHelper.lerp(delta, head.getX(), toPos.getX());
        double headY = MathHelper.lerp(delta, head.getY(), toPos.getY());
        double headZ = MathHelper.lerp(delta, head.getZ(), toPos.getZ());

        setHeadPos(headX, headY, headZ);
    }

    public void lerpHeadPos(int tick, int totalTicks, Vec3d toPos, Easing.Curve curve)
    {
        double c = curve.apply((double) tick / totalTicks);
        double headX = head.getX() + (toPos.getX() - head.getX()) * c;
        double headY = head.getY() + (toPos.getY() - head.getY()) * c;
        double headZ = head.getZ() + (toPos.getZ() - head.getZ()) * c;

        setHeadPos(headX, headY, headZ);
    }

    public void lerpHeadAngles(int tick, int totalTicks, float toPitch, float toYaw)
    {
        float delta = (float) tick / totalTicks;
        float headPitch = MathHelper.lerp(delta, head.getPitch(), toPitch);
        float headYaw = MathHelper.lerp(delta, head.getYaw(), toYaw);

        setHeadAngles(headPitch, headYaw);
    }

    public void returnHeadToNeutral(int tick, int totalTicks)
    {
        lerpHeadPos(tick, totalTicks, getNeutralHeadPos());
        Vec2f pitchYaw = getNeutralHeadPitch();
        lerpHeadAngles(tick, totalTicks, pitchYaw.x, pitchYaw.y);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers)
    {
        controllers.add(new AnimationController<>(this, "controller", 5, this::controller));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache()
    {
        return instanceCache;
    }


    public static class WormSegment extends Entity
    {
        protected final WormEntity parent;
        protected final EntityDimensions partDimensions;

        public WormSegment(WormEntity parent, float width, float height)
        {
            super(parent.getType(), parent.getWorld());
            this.parent = parent;
            this.partDimensions = EntityDimensions.changing(width, height);
            calculateDimensions();
        }

//        public WormSegment(EntityType<? extends WormSegment> type, World world)
//        {
//
//        }


        @Override
        protected void readCustomDataFromNbt(NbtCompound nbt)
        {
        }

        @Override
        protected void writeCustomDataToNbt(NbtCompound nbt)
        {
        }

        @Override
        public void setPitch(float pitch)
        {
            super.setPitch(pitch);
        }

        @Override
        public boolean isPartOf(Entity entity)
        {
            return this == entity || parent == entity;
        }

        @Override
        public Packet<ClientPlayPacketListener> createSpawnPacket()
        {
            throw new UnsupportedOperationException();
//            return new EntitySpawnS2CPacket(this);
        }

        @Override
        public void onSpawnPacket(EntitySpawnS2CPacket packet)
        {
            super.onSpawnPacket(packet);
        }

        @Override
        public EntityDimensions getDimensions(EntityPose pose)
        {
            return this.partDimensions;
        }

        @Override
        protected void initDataTracker()
        {

        }

        @Override
        public void updatePositionAndAngles(double x, double y, double z, float yaw, float pitch)
        {
            this.updatePosition(x, y, z);
            this.prevYaw = getYaw();
            this.prevPitch = getPitch();
            this.setYaw(yaw);
            this.setPitch(pitch);
        }

        @Override
        public void updatePosition(double x, double y, double z)
        {
            double d = MathHelper.clamp(x, -3.0E7, 3.0E7);
            double e = MathHelper.clamp(z, -3.0E7, 3.0E7);
            this.prevX = getX();
            this.prevY = getY();
            this.prevZ = getZ();
            this.setPosition(d, y, e);
        }

        @Override
        public boolean damage(DamageSource source, float amount)
        {
            return parent.damage(source, amount);
        }

        @Override
        public boolean shouldSave()
        {
            return false;
        }
    }
}