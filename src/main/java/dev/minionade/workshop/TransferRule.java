package dev.minionade.workshop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;

public final class TransferRule {
    public static final int ENABLED = 1;
    public static final int AUTO = 2;
    public static final int WHITELIST = 4;
    public static final Codec<TransferRule> CODEC = RecordCodecBuilder.create(instance -> instance.group(
        Codec.intRange(0, 7).optionalFieldOf("flags", 0).forGetter(rule -> rule.flags),
        Filter.CODEC.listOf(0, WorkshopLayout.FILTER_SIZE).optionalFieldOf("filters", List.of()).forGetter(rule -> rule.filters)
    ).apply(instance, TransferRule::new));

    private int flags;
    private final List<Filter> filters = new ArrayList<>(WorkshopLayout.FILTER_SIZE);

    public TransferRule() {
        this(0, List.of());
    }

    private TransferRule(int flags, List<Filter> filters) {
        this.flags = flags & 7;
        for (int i = 0; i < WorkshopLayout.FILTER_SIZE; i++) {
            this.filters.add(i < filters.size() ? filters.get(i) : new Filter(ItemStack.EMPTY, MatchMode.PRECISE));
        }
    }

    public int flags() {
        return flags;
    }

    public void toggle(int flag) {
        if (flag != ENABLED && flag != AUTO && flag != WHITELIST) {
            throw new IllegalArgumentException("Invalid transfer flag: " + flag);
        }
        flags ^= flag;
    }

    public boolean has(int flag) {
        return (flags & flag) != 0;
    }

    public Filter filter(int index) {
        return filters.get(index);
    }

    public void setFilter(int index, ItemStack stack) {
        filters.set(index, new Filter(stack.copyWithCount(1), filter(index).mode()));
    }

    public void cycleMode(int index) {
        Filter old = filter(index);
        MatchMode[] modes = MatchMode.values();
        filters.set(index, new Filter(old.item(), modes[(old.mode().ordinal() + 1) % modes.length]));
    }

    public boolean accepts(ItemStack stack, boolean filtering) {
        if (!filtering) {
            return true;
        }
        for (Filter filter : filters) {
            if (!filter.item().isEmpty() && filter.mode().matches(stack, filter.item())) {
                return has(WHITELIST);
            }
        }
        return !has(WHITELIST);
    }

    public record Filter(ItemStack item, MatchMode mode) {
        public static final Codec<Filter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ItemStack.OPTIONAL_CODEC.optionalFieldOf("item", ItemStack.EMPTY).forGetter(Filter::item),
            StringRepresentable.fromEnum(MatchMode::values).optionalFieldOf("mode", MatchMode.PRECISE).forGetter(Filter::mode)
        ).apply(instance, Filter::new));
    }

    public enum MatchMode implements StringRepresentable {
        PRECISE("precise"), ITEM("item"), FUZZY("fuzzy"), TAG("tag");

        private final String name;

        MatchMode(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public boolean matches(ItemStack stack, ItemStack filter) {
            return switch (this) {
                case PRECISE -> ItemStack.isSameItemSameComponents(stack, filter);
                case ITEM, FUZZY -> ItemStack.isSameItem(stack, filter);
                case TAG -> ItemStack.isSameItem(stack, filter)
                    || BuiltInRegistries.ITEM.wrapAsHolder(filter.getItem()).tags().anyMatch(stack::is);
            };
        }
    }
}
