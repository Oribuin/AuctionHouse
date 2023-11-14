package xyz.oribuin.auctionhouse.auction;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Optional;

public enum SortType {
    PRICE_ASCENDING("Price ↑", Comparator.comparingDouble(Auction::getPrice)),
    PRICE_DESCENDING("Price ↓", Comparator.comparingDouble(Auction::getPrice).reversed()),
    TIME_ASCENDING("Time Ascending ↑", Comparator.comparingLong(Auction::getCreatedTime).reversed()),
    TIME_DESCENDING("Time Descending ↓", Comparator.comparingLong(Auction::getCreatedTime));

    private final String displayName;
    private final Comparator<Auction> comparator;

    SortType(String displayName, Comparator<Auction> comparator) {
        this.displayName = displayName;
        this.comparator = comparator;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public Comparator<Auction> getComparator() {
        return comparator;
    }

    /**
     * Get the enum value from the display name
     *
     * @param name the display name
     * @return the enum value
     */
    public static Optional<SortType> match(String name) {
        return Arrays.stream(values()).filter(sort -> sort.name().equalsIgnoreCase(name)).findFirst();
    }

}
