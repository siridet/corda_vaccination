package net.corda.samples.tokenizedhouse.contracts;

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.samples.tokenizedhouse.states.FungibleVaccineTokenState;
import org.jetbrains.annotations.NotNull;

import static net.corda.core.contracts.ContractsDSL.requireThat;

/**
 * This doesn't do anything over and above the [EvolvableTokenContract].
 */
public class VaccineTokenStateContract extends EvolvableTokenContract implements Contract {

    public static final String CONTRACT_ID = "net.corda.samples.tokenizedhouse.contracts.VaccineTokenStateContract";

    @Override
    public void additionalCreateChecks(@NotNull LedgerTransaction tx) {
        // Write contract validation logic to be performed while creation of token
        FungibleVaccineTokenState outputState = (FungibleVaccineTokenState) tx.getOutput(0);
        requireThat( require -> {
            require.using("Valuation must be more than 0",
                    outputState.getValuation() > 1);
            return null;
        });
    }

    @Override
    public void additionalUpdateChecks(@NotNull LedgerTransaction tx) {
        // Write contract validation logic to be performed while updation of token
    }
}
