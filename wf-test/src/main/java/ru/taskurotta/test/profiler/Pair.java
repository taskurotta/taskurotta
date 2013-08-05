package ru.taskurotta.test.profiler;

/**
 * Created by void 02.08.13 17:25
 */
public class Pair<TLeft, TRight> {
    TLeft left;
    TRight right;

    public Pair() {
    }

    public Pair(TLeft left, TRight right) {
        this.left = left;
        this.right = right;
    }

    public TLeft getLeft() {
        return left;
    }

    public void setLeft(TLeft left) {
        this.left = left;
    }

    public TRight getRight() {
        return right;
    }

    public void setRight(TRight right) {
        this.right = right;
    }
}
