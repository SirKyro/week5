package umlang.value;

/**
 * A VCell is a MUTABLE cell value.
 * 
 * The default Object.equals() method is appropriate: it compares objects by pointer-identity.
 */
public final class VCell implements Value {
    private Value _contents;

    public VCell(Value v) {
        this._contents = v;
    }

    /** Retrieve the current value held in the cell. */
    public Value contents() {
        return this._contents;
    }

    /** Update the value stored in this cell. */
    public void setContents(Value newValue) {
        this._contents = newValue;
    }

    // We don't *really* have to override this, as structural equivalence AS IMPLEMENTED is pointer equivalence, which IS appropriate, but we do so for consistency.
    @Override
    public boolean isStructuralEquivalenceAppropriate() {
        return false;
    }
}