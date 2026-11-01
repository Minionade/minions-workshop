//? if neoforge {
/*package dev.minionade.workshop;

import static org.junit.jupiter.api.Assertions.*;
import dev.minionade.workshop.neoforge.WorkshopIndustrialEnergy;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
//? if >=26.1 {
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
//? } else {
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
//? }

class WorkshopIndustrialEnergyTest {
    @BeforeAll
    static void bootstrap() {
        CraftingAndTransferTest.bootstrapMinecraft();
    }

    //? if >=26.1 {
    public interface ModernEnergy extends EnergyHandler {
        boolean canConnect(String tier);
        default boolean supportsInsertion() { return true; }
        default boolean supportsExtraction() { return true; }
    }

    @Test
    void modernEuUsesTheSameJournalAsNativeEnergyAndFluid() {
        WorkshopBlockEntity table = WorkshopResourceTest.table();
        WorkshopResourceTest.enable(table);
        ModernEnergy storage = WorkshopIndustrialEnergy.storage(ModernEnergy.class, table, Direction.NORTH, () -> 10);
        assertTrue(storage.canConnect("lv"));
        assertTrue(storage.supportsInsertion());
        assertFalse(storage.supportsExtraction());
        assertEquals(6400, storage.getCapacityAsInt());
        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(1, storage.insert(1, transaction));
            assertEquals(1, table.resourceHandlers.energy(Direction.NORTH).insert(1, transaction));
            assertEquals(0, storage.extract(1, transaction));
        }
        assertEquals(0, table.power().energy());
        try (Transaction transaction = Transaction.openRoot()) {
            assertEquals(1, storage.insert(1, transaction));
            transaction.commit();
        }
        assertEquals(10, table.power().energy());
        assertEquals(1, storage.getAmountAsInt());
        assertEquals(1, table.fuel());
        table.removeItem(WorkshopLayout.GLOBAL, 1);
        assertFalse(storage.supportsInsertion());
        assertFalse(storage.canConnect("lv"));
    }
    //? } else {
    @Test
    void actualPublishedMiAndSiApiInterfacesReceiveSimulateAndDisableCorrectly() throws Exception {
        String jars = System.getProperty("workshop.industrialApiJars");
        assertNotNull(jars, "Real MI/SI API jars must be supplied by the native test task");
        URL[] urls = Arrays.stream(jars.split(java.util.regex.Pattern.quote(File.pathSeparator)))
            .map(path -> {
                try {
                    return new File(path).toURI().toURL();
                } catch (java.net.MalformedURLException error) {
                    throw new IllegalStateException(error);
                }
            }).toArray(URL[]::new);
        try (URLClassLoader loader = new URLClassLoader(urls, getClass().getClassLoader())) {
            verify(loader, "aztech.modern_industrialization.api.energy.MIEnergyStorage", 10);
            verify(loader, "com.miophas.singularity_iteration.core.api.energy.storage.IEUEnergyStorage", 4);
        }
    }

    private static void verify(ClassLoader loader, String name, int ratio) throws Exception {
        Class<?> type = Class.forName(name, true, loader);
        WorkshopBlockEntity table = WorkshopResourceTest.table();
        WorkshopResourceTest.enable(table);
        Object storage = WorkshopIndustrialEnergy.storage(type, table, Direction.NORTH, () -> ratio);
        var receive = type.getMethod("receive", long.class, boolean.class);
        var amount = type.getMethod("getAmount");
        assertEquals(64_000L / ratio, receive.invoke(storage, Long.MAX_VALUE, true));
        assertEquals(0, table.power().energy());
        assertEquals(1L, receive.invoke(storage, 1L, false));
        assertEquals(ratio, table.power().energy());
        assertEquals(1L, amount.invoke(storage));
        assertEquals(64_000L / ratio, type.getMethod("getCapacity").invoke(storage));
        assertEquals(0L, type.getMethod("extract", long.class, boolean.class).invoke(storage, 1L, false));
        assertEquals(false, type.getMethod("canExtract").invoke(storage));
        assertEquals(true, type.getMethod("canConnect", String.class).invoke(storage, "lv"));
        assertEquals(false, type.getMethod("canConnect", String.class).invoke(storage, "invalid"));
        assertEquals(storage, storage);
        assertNotNull(storage.toString());
        InvocationTargetException error = assertThrows(InvocationTargetException.class,
            () -> receive.invoke(storage, -1L, false));
        assertInstanceOf(IllegalArgumentException.class, error.getCause());
        table.removeItem(WorkshopLayout.GLOBAL, 1);
        assertEquals(false, type.getMethod("canReceive").invoke(storage));
        assertEquals(false, type.getMethod("canConnect", String.class).invoke(storage, "lv"));
        assertEquals(0L, receive.invoke(storage, 100L, false));
        assertEquals(ratio, table.power().energy());
    }
    //? }

    public interface InvalidEnergy {
        void unknownOperation();
    }

    public interface InvalidSignature {
        long receive(long maximum, int simulate);
    }

    @Test
    void unsupportedIndustrialApiChangesFailExplicitly() {
        assertThrows(IllegalStateException.class, () -> WorkshopIndustrialEnergy.storage(
            InvalidEnergy.class, WorkshopResourceTest.table(), Direction.NORTH, () -> 10));
        assertThrows(IllegalStateException.class, () -> WorkshopIndustrialEnergy.storage(
            InvalidSignature.class, WorkshopResourceTest.table(), Direction.NORTH, () -> 10));
    }
}
*///? }
