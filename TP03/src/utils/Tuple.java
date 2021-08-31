package utils;

public class Tuple<X, Y> {
    private X key;
    private Y value;

    public Tuple(X key, Y value) {
        this.key = key;
        this.value = value;
    }

    public X getKey() {
        return key;
    }

    public Y getValue() {
        return value;
    }

    public void setKey(X key) {
        this.key = key;
    }

    public void setValue(Y value) {
        this.value = value;
    }
    
}
