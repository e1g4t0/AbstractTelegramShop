package kz.shopTelegramBot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Customer {

    static BotDriver bot;
    static DataBase db;
    private static final Map<String, String> getenv = System.getenv();


    public Customer(BotDriver bot, DataBase db) {
        Customer.bot = bot;
        Customer.db = db;
    }

    public void purchaseProduct(User from, int goodsId) throws TelegramApiException {
        checkUser(from);
        HashMap<String, Object> productData = db.doesProductAvailable(goodsId);
        if ((int) productData.get("G_Count") > 0) {
            System.out.println((int) productData.get("G_Count") - 1);
            db.updateCount((int) productData.get("GoodsID"), (int) productData.get("G_Count") - 1);

            bot.execute(SendMessage.builder().chatId(from.getId())
                    .text("Congrats ! You are all set!").replyMarkup(bot.backButton()).build());

            String updMessage = String.format("<b>%s %s GB</b>%n%n<u>" +
                            "Characteristics:</u> CPU - %s, RAM - %s, %s grams %n%n $%s %n%s goods left",
                    productData.get("G_Name"), productData.get("G_Memory"), productData.get("G_CPU"),
                    productData.get("RAM"), productData.get("Weight"), productData.get("Price"),
                    (int) productData.get("G_Count") - 1);

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(bot.addInlineButton("Purchase", "purchase_" + productData.get("GoodsID")));
            inlineKeyboardMarkup.setKeyboard(rows);

            db.purchaseProduct(from.getId(), (int) productData.get("GoodsID"));

            bot.execute(EditMessageText.builder().chatId(getenv.get("CHANNEL_ID"))
                    .messageId((int) productData.get("Msg_ID")).text(updMessage)
                    .replyMarkup(inlineKeyboardMarkup).parseMode("HTML").build());

        } else {
            bot.execute(SendMessage.builder().chatId(from.getId())
                    .text("Product doesn't available").replyMarkup(bot.backButton()).build());
            return;
        }
        sendUserMainMenu(from.getId());
    }

    public void sendUserMainMenu(Long id) throws TelegramApiException {
        SendMessage sendMessage = new SendMessage(String.valueOf(id), "Main menu");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(bot.addInlineButton("My shopping history", "shopping_history"));
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton inline = new InlineKeyboardButton("Purchase");
        inline.setUrl("https://t.me/javaShopOnline");
        row.add(inline);
        rows.add(row);

        inlineKeyboardMarkup.setKeyboard(rows);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        bot.execute(sendMessage);
    }

    public void getShoppingHistory(User from) throws TelegramApiException {
        String data;
        try {
            data = db.getCartData(from.getId());
            bot.execute(SendMessage.builder().chatId(from.getId())
                    .text(data).build());
            sendUserMainMenu(from.getId());
        } catch (SQLException ignored) {
        }

    }

    public void checkUser(User user) {
        HashMap<String, Object> userData = new HashMap<>();

        userData.put("user_id", user.getId());
        userData.put("f_name", user.getFirstName());
        userData.put("l_name", user.getLastName());
        userData.put("username", user.getUserName());

        db.addUser(userData);

    }
}
