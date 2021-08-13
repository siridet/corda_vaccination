package net.corda.samples.tokenizedhouse.contracts;

import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.samples.tokenizedhouse.states.FungibleVaccineTokenState;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class ContractTests {
    private final MockServices ledgerServices = new MockServices();
    TestIdentity Operator = new TestIdentity(new CordaX500Name("Alice",  "TestLand",  "US"));

    //sample tests
    @Test
    public void valuationCannotBeZero() {
        FungibleVaccineTokenState tokenPass = new FungibleVaccineTokenState(10000,Operator.getParty(),
                new UniqueIdentifier(),
                0,"NYCHelena");
        FungibleVaccineTokenState tokenFail = new FungibleVaccineTokenState(0,Operator.getParty(),
                new UniqueIdentifier(),
                0,"NYCHelena");
        ledger(ledgerServices, l -> {
            l.transaction(tx -> {
                tx.output(VaccineTokenStateContract.CONTRACT_ID, tokenFail);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create());
                return tx.fails();
            });
            l.transaction(tx -> {
                tx.output(VaccineTokenStateContract.CONTRACT_ID, tokenPass);
                tx.command(Operator.getPublicKey(), new com.r3.corda.lib.tokens.contracts.commands.Create());
                return tx.verifies();
            });
            return null;
        });
    }
}