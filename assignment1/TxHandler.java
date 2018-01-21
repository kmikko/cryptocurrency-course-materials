import java.lang.reflect.Array;
import java.util.ArrayList;

public class TxHandler {

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    private UTXOPool utxoPool;
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        UTXOPool tempPool = new UTXOPool();
        double totalIn = 0;
        double totalOut = 0;

        for (int i=0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            UTXO tempUTXO = new UTXO(input.prevTxHash, input.outputIndex);

            // (1) all outputs claimed by tx are in the current UTXO pool
            // i.e. all inputs in the tx are in the current UTXO pool
            if (!this.utxoPool.contains(tempUTXO)) {
                return false;
            }

            // (2) the signatures on each input of tx are valid
            Transaction.Output output = this.utxoPool.getTxOutput(tempUTXO);
            if (!Crypto.verifySignature(output.address, tx.getRawDataToSign(i), input.signature)) {
                return false;
            }

            // (3) no UTXO is claimed multiple times by tx
            // i.e. input not using same output more than once
            if (tempPool.contains(tempUTXO)) {
                return false;
            }
            tempPool.addUTXO(tempUTXO, tx.getOutput(i));

            // Input's value i.e. output's value it's using
            totalIn += output.value;
        }

        for (Transaction.Output output : tx.getOutputs()) {
            // (4) all of txs output values are non-negative
            if (output.value < 0) {
                return false;
            }
            totalOut += output.value;
        }

        // (5) the sum of txs input values is greater than or equal to the sum of its output
        // values; and false otherwise.
        return totalIn >= totalOut;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> validTxs = new ArrayList<>();
        for (Transaction tx : possibleTxs) {
            if (isValidTx(tx)) {
                validTxs.add(tx);

                // Remove used ouputs from UTXO pool
                for (Transaction.Input input : tx.getInputs()) {
                    UTXO tempUTXO = new UTXO(input.prevTxHash, input.outputIndex);
                    this.utxoPool.removeUTXO(tempUTXO);
                }

                // Add new outputs to UTXO pool
                for (int i=0; i < tx.numOutputs(); i++) {
                    Transaction.Output output = tx.getOutput(i);
                    UTXO utxo= new UTXO(tx.getHash(), i);
                    this.utxoPool.addUTXO(utxo, output);
                }
            }
        }
        return validTxs.toArray(new Transaction[validTxs.size()]);
    }

}
