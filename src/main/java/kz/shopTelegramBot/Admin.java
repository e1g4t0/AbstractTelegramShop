package kz.shopTelegramBot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Admin {

    private static final Map<String, String> getenv = System.getenv();

    static BotDriver bot;
    static DataBase db;

    static AdminActions adminAct = AdminActions.EMPTY;


    public Admin(BotDriver bot, DataBase db) {
        Admin.bot = bot;
        Admin.db = db;
    }

    private void addProduct(Message message) throws TelegramApiException {

        String[] data = message.getText().split("\n");

        try {
            int id = db.addProduct(data);
            String text = String.format("<b>%s %s GB</b>%n%n<u>" +
                            "Characteristics:</u> CPU - %s, RAM - %s, %s grams %n%n $%s %n%s goods left",
                    data[2], data[5], data[3], data[4], data[6], data[0], data[1]);
            SendMessage sendMessage = new SendMessage(getenv.get("CHANNEL_ID"), text);
            sendMessage.setParseMode("HTML");

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(bot.addInlineButton("Purchase", "purchase_" + id));

            inlineKeyboardMarkup.setKeyboard(rows);
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);
            Message sentMessage = bot.execute(sendMessage);
            db.setMsgId(sentMessage.getMessageId());

        } catch (SQLException e) {
            bot.execute(SendMessage.builder().chatId(message.getChatId())
                    .text("<u><b>Your data is incorrect.</b></u>\nSend me a product data separated by spaces." +
                            "\n\n<b>Instruction:</b>\n<i>1.Price;\n2.Count;\n3.Name;\n" +
                            "4.CPU;\n5.RAM;\n6.Memory;\n7.Weight\n</i>").parseMode("HTML").replyMarkup(bot.backButton()).build());
        }

        adminAct = AdminActions.EMPTY;
        bot.execute(SendMessage.builder().chatId(message.getChatId())
                .text("<i>Success!</i>").parseMode("HTML").build());

        sendAdminMainMenu();

    }

    private void updateProduct(Message message) throws TelegramApiException {
        try {
            int id = adminAct.getId();
            HashMap<String, String> msgData = db.updateCount(id, Integer.parseInt(message.getText()));
            String updMessage = formatProductData(msgData);

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            rows.add(bot.addInlineButton("Purchase", "purchase_" + id));
            inlineKeyboardMarkup.setKeyboard(rows);

            bot.execute(EditMessageText.builder().chatId(getenv.get("CHANNEL_ID"))
                    .messageId(Integer.parseInt(msgData.get("Msg_ID"))).text(updMessage)
                    .replyMarkup(inlineKeyboardMarkup).parseMode("HTML").build());

            bot.execute(SendMessage.builder().chatId(message.getChatId())
                    .text("<i>Success!</i>").parseMode("HTML").build());

            sendAdminMainMenu();
        } catch (final NumberFormatException e) {
            bot.execute(SendMessage.builder().chatId(getenv.get("ADMIN_ID"))
                    .text("Please text me a numeric value").replyMarkup(bot.backButton()).build());
        }

    }


    private void findProduct(String message) throws TelegramApiException {
        try {
            int id = Integer.parseInt(message);
            String productData = db.findProduct(id);
            if (productData.isEmpty()) {
                bot.execute(SendMessage.builder().chatId(getenv.get("ADMIN_ID"))
                        .text(productData + "I can't find this product. Please, try again.")
                        .replyMarkup(bot.backButton()).build());
            } else {
                bot.execute(SendMessage.builder().chatId(getenv.get("ADMIN_ID"))
                        .text(productData + ". Please text me new count of product")
                        .replyMarkup(bot.backButton()).build());
                AdminActions.UPDATE_PRODUCT.setId(id);
                adminAct = AdminActions.UPDATE_PRODUCT;
            }
        } catch (final NumberFormatException e) {
            bot.execute(SendMessage.builder().chatId(getenv.get("ADMIN_ID"))
                    .text("It's not id. Please text me a numeric value")
                    .replyMarkup(bot.backButton()).build());
        }
    }

    public void sendAdminMainMenu() throws TelegramApiException {
        adminAct = AdminActions.EMPTY;
        long chatId = Long.parseLong(getenv.get("ADMIN_ID"));
        SendMessage sendMessage = new SendMessage(String.valueOf(chatId), "Main menu");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(bot.addInlineButton("Add a product", "add_product"));
        rows.add(bot.addInlineButton("Update a product", "find_product"));

        inlineKeyboardMarkup.setKeyboard(rows);
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        bot.execute(sendMessage);
    }

    public boolean checkAdminAct(Message message) throws TelegramApiException {
        if (adminAct == AdminActions.EMPTY) {
            return false;
        }

        switch (adminAct) {
            case ADD_PRODUCT:
                addProduct(message);
                return true;
            case FIND_PRODUCT:
                findProduct(message.getText());
                return true;
            case UPDATE_PRODUCT:
                updateProduct(message);
                return true;
        }
        return false;
    }

    public String formatProductData(HashMap<String, String> data) {
        return String.format("<b>%s %s GB</b>%n%n<u>" +
                        "Characteristics:</u> CPU - %s, RAM - %s, %s grams %n%n $%s %n%s goods left",
                data.get("G_Name"), data.get("G_Memory"), data.get("G_CPU"),
                data.get("RAM"), data.get("Weight"), data.get("Price"),
                data.get("G_Count"));
    }

}
