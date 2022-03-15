package onl.tesseract.commandBuilder;

public final class Pair<LEFT, RIGHT> {

    private final LEFT left;
    private final RIGHT right;

    public Pair(final LEFT left, final RIGHT right)
    {
        this.left = left;
        this.right = right;
    }

    public LEFT getLeft()
    {
        return left;
    }

    public RIGHT getRight()
    {
        return right;
    }
}
