package net.corda.samples.tokenizedhouse.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.rpc.*;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.samples.tokenizedhouse.states.FungibleVaccineTokenState;
import kotlin.Unit;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

/**
 * Create,Issue,Move,Redeem token flows for a house asset on ledger
 * This is all-in-one implementation style.
 */
public class RealEstateEvolvableFungibleTokenFlow {

    /**
     * Create Fungible Token for a house asset on ledger
     */
    @StartableByRPC
    public static class BuyVaccineTokenFlow extends FlowLogic<SignedTransaction> {

        // valuation property of a house can change hence we are considering house as a evolvable asset
        private final int valuation;
        private final String symbol;

        public BuyVaccineTokenFlow(String symbol, int valuation) {
            this.valuation = valuation;
            this.symbol = symbol;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to a notary we wish to use.
            /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
             *
             *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
             */
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
            // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

            //create token type
            FungibleVaccineTokenState evolvableTokenType = new FungibleVaccineTokenState(valuation, getOurIdentity(),
                    new UniqueIdentifier(), 0, this.symbol);

            //wrap it with transaction state specifying the notary
            TransactionState<FungibleVaccineTokenState> transactionState = new TransactionState<>(evolvableTokenType, notary);

            //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
            return subFlow(new CreateEvolvableTokens(transactionState));
        }
    }

    /**
     *  Issue Fungible Token against an evolvable house asset on ledger
     */
    @StartableByRPC
    public static class IssueVaccineTokenFlow extends FlowLogic<SignedTransaction>{
        private final String symbol;
        private final int quantity;
        private final Party holder;

        public IssueVaccineTokenFlow(String symbol, int quantity, Party holder) {
            this.symbol = symbol;
            this.quantity = quantity;
            this.holder = holder;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //get house states on ledger with uuid as input tokenId
            StateAndRef<FungibleVaccineTokenState> stateAndRef = getServiceHub().getVaultService().
                    queryBy(FungibleVaccineTokenState.class).getStates().stream()
                    .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).findAny()
                    .orElseThrow(()-> new IllegalArgumentException("FungibleHouseTokenState symbol=\""+symbol+"\" not found from vault"));

            //get the RealEstateEvolvableTokenType object
            FungibleVaccineTokenState evolvableTokenType = stateAndRef.getState().getData();

            //create fungible token for the house token type
            FungibleToken fungibleToken = new FungibleTokenBuilder()
                    .ofTokenType(evolvableTokenType.toPointer(FungibleVaccineTokenState.class)) // get the token pointer
                    .issuedBy(getOurIdentity())
                    .heldBy(holder)
                    .withAmount(quantity)
                    .buildFungibleToken();

            //use built in flow for issuing tokens on ledger
            return subFlow(new IssueTokens(ImmutableList.of(fungibleToken)));
        }
    }

    /**
     *  Move created fungible tokens to other party
     */
    @StartableByRPC
    @InitiatingFlow
    public static class InjectTokenFlow extends FlowLogic<SignedTransaction>{
        private final String symbol;
        private final Party holder;
        private final int quantity;

        public InjectTokenFlow(String symbol, Party holder, int quantity) {
            this.symbol = symbol;
            this.holder = holder;
            this.quantity = quantity;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //get house states on ledger with uuid as input tokenId
            StateAndRef<FungibleVaccineTokenState> stateAndRef = getServiceHub().getVaultService().
                    queryBy(FungibleVaccineTokenState.class).getStates().stream()
                    .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).findAny()
                    .orElseThrow(()-> new IllegalArgumentException("FungibleHouseTokenState=\""+symbol+"\" not found from vault"));

            //get the RealEstateEvolvableTokenType object
            FungibleVaccineTokenState tokenstate = stateAndRef.getState().getData();

            /*  specify how much amount to transfer to which holder
             *  Note: we use a pointer of tokenstate because it of type EvolvableTokenType
             */
            Amount<TokenType> amount = new Amount<>(quantity, tokenstate.toPointer(FungibleVaccineTokenState.class));
            //PartyAndAmount partyAndAmount = new PartyAndAmount(holder, amount);

            //use built in flow to move fungible tokens to holder
            return subFlow(new MoveFungibleTokens(amount,holder));
        }
    }

    @InitiatedBy(InjectTokenFlow.class)
    public static class MoveEvolvableFungibleTokenFlowResponder extends FlowLogic<Unit>{

        private FlowSession counterSession;

        public MoveEvolvableFungibleTokenFlowResponder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            // Simply use the MoveFungibleTokensHandler as the responding flow
            return subFlow(new MoveFungibleTokensHandler(counterSession));
        }
    }
}

