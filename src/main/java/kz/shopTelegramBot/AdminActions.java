package kz.shopTelegramBot;

public enum AdminActions {
    ADD_PRODUCT,
    FIND_PRODUCT,
    UPDATE_PRODUCT(-1),
    EMPTY();
    private int id;

    AdminActions(int id) {
        this.id = id;
    }

    AdminActions() {}

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
