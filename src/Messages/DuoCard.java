package Messages;

public class DuoCard {
    public String color; // e.g., RED, GREEN, BLUE, YELLOW, WILD
    public String value; // e.g., 0-9, SKIP, REVERSE, DRAW2, WILD, WILD4
    public String wildColor; // when played, the color chosen for WILD or WILD4 cards

    public DuoCard() {}

    public DuoCard(String color, String value, String wildColor) {
        this.color = color;
        this.value = value;
        this.wildColor = wildColor;
    }

    public DuoCard(String color, String value) {
        this.color = color;
        this.value = value;
        this.wildColor = null;
    }

    public static DuoCard clearWildColor(DuoCard card) {
        return new DuoCard(card.color, card.value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DuoCard other = (DuoCard) o;

        if (color == null) {
            if (other.color != null) return false;
        } else if (!color.equals(other.color)) return false;

        if (value == null) {
            if (other.value != null) return false;
        } else if (!value.equals(other.value)) return false;

        if (wildColor == null) {
            return other.wildColor == null;
        } else {
            return wildColor.equals(other.wildColor);
        }
    }

    @Override
    public int hashCode() {
        int result = (color == null) ? 0 : color.hashCode();
        result = 31 * result + ((value == null) ? 0 : value.hashCode());
        result = 31 * result + ((wildColor == null) ? 0 : wildColor.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "DuoCard{" +
                "color='" + color + '\'' +
                ", value='" + value + '\'' +
                (wildColor != null ? ", wildColor='" + wildColor + '\'' : "") +
                '}';
    }
}
