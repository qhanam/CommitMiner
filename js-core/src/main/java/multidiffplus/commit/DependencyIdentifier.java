package multidiffplus.commit;

import java.util.List;

/**
 * An identifier for linking criterion and dependencies in an abstract slice.
 */
public interface DependencyIdentifier {

    /**
     * @return A string representation of the address.
     */
    String getAddress();

    /**
     * @return Integer representations of the addresses.
     */
    List<Integer> getAddresses();

}