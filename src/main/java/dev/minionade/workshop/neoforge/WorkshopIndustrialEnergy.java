//? if neoforge {
/*package dev.minionade.workshop.neoforge;

import dev.minionade.workshop.Workshop;
import dev.minionade.workshop.WorkshopBlockEntity;
import dev.minionade.workshop.WorkshopPower;
import dev.minionade.workshop.WorkshopRules;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.function.IntSupplier;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.ModConfigSpec;
//? if >=26.1 {
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
//? }

public final class WorkshopIndustrialEnergy {
    private WorkshopIndustrialEnergy() {}

    public static void register(RegisterCapabilitiesEvent event) {
        try {
            if (ModList.get().isLoaded("modern_industrialization")) {
                Class<?> storage = Class.forName("aztech.modern_industrialization.api.energy.MIEnergyStorage");
                Class<?> config = Class.forName("aztech.modern_industrialization.config.MIServerConfig");
                Object instance = config.getField("INSTANCE").get(null);
                Object value = config.getField("forgeEnergyPerEu").get(instance);
                if (!(value instanceof ModConfigSpec.IntValue ratio)) {
                    throw new IllegalStateException("MI forgeEnergyPerEu is not an integer config value");
                }
                register(event, storage, "modern_industrialization", "sided_mi_energy_storage", ratio::getAsInt);
            }
            if (ModList.get().isLoaded("mio_icif")) {
                Class<?> storage = Class.forName("com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage");
                int ratio = storage.getField("FE_PER_EU").getInt(null);
                register(event, storage, "mio_icif", "sided_eu_energy_storage", () -> ratio);
            }
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Installed industrial energy mod has an incompatible public API", error);
        }
    }

    private static <T> void register(RegisterCapabilitiesEvent event, Class<T> type,
        String namespace, String path, IntSupplier conversion) {
        validate(type);
        BlockCapability<T, Direction> capability = BlockCapability.createSided(
            ResourceLocation.fromNamespaceAndPath(namespace, path), type);
        event.registerBlockEntity(capability, Workshop.TABLE_ENTITY, (table, side) -> {
            if (side == null) {
                return null;
            }
            return storage(type, table, side, conversion);
        });
        Workshop.LOGGER.info("Registered {} EU input for Minion's Workshop", namespace);
    }

    // These APIs are optional, and MI changes from simulation to transactions in 26.1.
    // Implement the installed public interface without bundling either mod's classes.
    public static <T> T storage(Class<T> type, WorkshopBlockEntity table, Direction side, IntSupplier conversion) {
        validate(type);
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type},
            (proxy, method, args) -> invoke(proxy, method, args, table, side, conversion)));
    }

    private static void validate(Class<?> type) {
        if (!type.isInterface()) {
            throw new IllegalStateException("Industrial energy storage is not an interface: " + type.getName());
        }
        for (Method method : type.getMethods()) {
            if (!method.isDefault() && !Modifier.isStatic(method.getModifiers()) && !supported(method)) {
                throw new IllegalStateException("Unsupported industrial energy API method: " + method);
            }
        }
    }

    private static boolean supported(Method method) {
        Class<?>[] parameters = method.getParameterTypes();
        Class<?> result = method.getReturnType();
        return switch (method.getName()) {
            case "canConnect" -> result == boolean.class && parameters.length == 1
                && (parameters[0] == String.class || parameters[0].getSimpleName().equals("CableTier"));
            case "canReceive", "canExtract", "supportsInsertion", "supportsExtraction" ->
                result == boolean.class && parameters.length == 0;
            case "getAmount", "getCapacity", "getAmountAsLong", "getCapacityAsLong" ->
                result == long.class && parameters.length == 0;
            case "receive" -> result == long.class && Arrays.equals(parameters, new Class<?>[] {long.class, boolean.class});
            case "extract" -> result == long.class && Arrays.equals(parameters, new Class<?>[] {long.class, boolean.class})
                || transactional(method);
            case "insert" -> transactional(method);
            default -> false;
        };
    }

    private static boolean transactional(Method method) {
        Class<?>[] parameters = method.getParameterTypes();
        return method.getReturnType() == int.class && parameters.length == 2 && parameters[0] == int.class
            && parameters[1].getName().equals("net.neoforged.neoforge.transfer.transaction.TransactionContext");
    }

    private static Object invoke(Object proxy, Method method, Object[] args,
        WorkshopBlockEntity table, Direction side, IntSupplier conversion) throws Throwable {
        String name = method.getName();
        if (method.getDeclaringClass() == Object.class) {
            return switch (name) {
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "Workshop EU input at " + table.getBlockPos() + " [" + side + "]";
                default -> throw new UnsupportedOperationException(method.toString());
            };
        }
        if (name.equals("canConnect") && method.getParameterTypes()[0] == String.class && method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        switch (name) {
            case "canConnect":
                return args[0] != null && table.canReceiveEnergy(side);
            case "canReceive", "supportsInsertion":
                return table.canReceiveEnergy(side);
            case "canExtract", "supportsExtraction":
                return false;
            case "getAmount", "getAmountAsLong":
                return (long) table.power().energy() / ratio(conversion);
            case "getCapacity", "getCapacityAsLong":
                return (long) WorkshopRules.MAX_FUEL * WorkshopPower.ENERGY_PER_FUEL / ratio(conversion);
            case "extract":
                checkAmount(((Number) args[0]).longValue());
                if (method.getReturnType() == int.class) {
                    return Integer.valueOf(0);
                }
                return Long.valueOf(0);
            case "receive":
                long maximum = ((Number) args[0]).longValue();
                checkAmount(maximum);
                if (!table.canReceiveEnergy(side)) {
                    return 0L;
                }
                boolean simulate = (boolean) args[1];
                long accepted = table.power().insertEnergy(maximum, ratio(conversion), simulate);
                if (accepted > 0 && !simulate) {
                    table.setChanged();
                }
                return accepted;
            //? if >=26.1 {
            case "insert":
                return table.resourceHandlers.insertEnergyUnits(side, (int) args[0], ratio(conversion),
                    (TransactionContext) args[1]);
            //? }
            default:
                if (method.isDefault()) {
                    return InvocationHandler.invokeDefault(proxy, method, args);
                }
                throw new UnsupportedOperationException("Unsupported industrial energy API method: " + method);
        }
    }

    private static int ratio(IntSupplier conversion) {
        int ratio = conversion.getAsInt();
        if (ratio <= 0) {
            throw new IllegalStateException("Industrial energy conversion ratio must be positive: " + ratio);
        }
        return ratio;
    }

    private static void checkAmount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Resource amount must not be negative");
        }
    }
}
*///? }
