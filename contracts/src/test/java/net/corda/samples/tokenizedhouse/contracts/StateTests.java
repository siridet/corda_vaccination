package net.corda.samples.tokenizedhouse.contracts;

import net.corda.samples.tokenizedhouse.states.FungibleVaccineTokenState;
import net.corda.testing.node.MockServices;
import org.junit.Test;

public class StateTests {
    private final MockServices ledgerServices = new MockServices();

    //sample State tests
    @Test
    public void hasConstructionAreaFieldOfCorrectType() throws NoSuchFieldException {
        // Does the message field exist?
        FungibleVaccineTokenState.class.getDeclaredField("symbol");
        // Is the message field of the correct type?
        assert(FungibleVaccineTokenState.class.getDeclaredField("symbol").getType().equals(String.class));
    }
}