package kz.shopTelegramBot;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.util.*;

public class BotDriver extends TelegramLongPollingBot {

    private static final Map<String, String> getenv = System.getenv();
    static BotDriver bot = new BotDriver();
    static Admin admin;
    static Customer customer;


    @Override
    public String getBotUsername() {
        return getenv.get("BOT_NAME");
    }

    @Override
    public String getBotToken() {
        return getenv.get("BOT_TOKEN");
    }


    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage()) {
                Message message = update.getMessage();
                if (message.hasText() && message.hasEntities()) {
                    commandHandle(message);
                } else if (message.hasText() && getenv.get("ADMIN_ID").equals(message.getChatId().toString())) {
                    adminHandler(message);
                } else if (message.hasText() && !getenv.get("ADMIN_ID").equals(message.getChatId().toString())) {
                    customer.sendUserMainMenu(message.getChatId());
                }
            } else if (update.hasCallbackQuery()) {
                callbackQueryHandle(update.getCallbackQuery());
            }
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }


    private void callbackQueryHandle(CallbackQuery callbackQuery) throws TelegramApiException {
        String callbackData = callbackQuery.getData();
        int goodsId = 0;
        if (callbackData.startsWith("purchase_")) {
            System.out.println(callbackData.replace("purchase_", ""));
            goodsId = Integer.parseInt(callbackData.replace("purchase_", ""));
            callbackData = "purchase";
            System.out.println(callbackData);
        }

        switch (callbackData) {
            case "add_product":
                execute(SendMessage.builder().chatId(callbackQuery.getFrom().getId())
                        .text("Send me a product data separated by spaces." +
                                "\n\n<b>Instruction:</b>\n<i>1.Price;\n2.Count;\n3.Name;\n" +
                                "4.CPU;\n5.RAM;\n6.Memory;\n7.Weight\n</i>")
                        .parseMode("HTML").replyMarkup(backButton()).build());
                Admin.adminAct = AdminActions.ADD_PRODUCT;
                break;
            case "find_product":
                execute(SendMessage.builder().chatId(callbackQuery.getFrom().getId())
                        .text("Send me a product id").build());
                Admin.adminAct = AdminActions.FIND_PRODUCT;
                break;
            case "home":
                Admin.adminAct = AdminActions.EMPTY;
                admin.sendAdminMainMenu();
                break;
            case "purchase":
                customer.purchaseProduct(callbackQuery.getFrom(), goodsId);
                break;
            case "shopping_history":
                customer.getShoppingHistory(callbackQuery.getFrom());
                break;
        }
    }


    private void commandHandle(Message message) throws TelegramApiException {
        Optional<MessageEntity> commandEntity =
                message.getEntities().stream().filter(e -> "bot_command".equals(e.getType())).findFirst();
        if (commandEntity.isPresent()) {
            String command =
                    message.getText().substring(commandEntity.get().getOffset(), commandEntity.get().getLength());
            if ("/start".equals(command)) {
                if (getenv.get("ADMIN_ID").equals(String.valueOf(message.getChatId()))) {
                    admin.sendAdminMainMenu();
                } else {
                    customer.checkUser(message.getFrom());
                    customer.sendUserMainMenu(message.getChatId());
                }
            }
        }
    }


    private void adminHandler(Message message) {
        try {
            if (admin.checkAdminAct(message)) {
                return;
            }
            admin.sendAdminMainMenu();

        } catch (TelegramApiException tae) {
            throw new RuntimeException(tae);
        }
    }


    public List<InlineKeyboardButton> addInlineButton(String text, String callbackData) {
        List<InlineKeyboardButton> row = new ArrayList<>();
        InlineKeyboardButton inline = new InlineKeyboardButton(text);
        inline.setCallbackData(callbackData);
        row.add(inline);
        return row;
    }


    public InlineKeyboardMarkup backButton() {
        InlineKeyboardMarkup backButton = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> backRow = new ArrayList<>();
        backRow.add(addInlineButton("Back to Main Menu", "home"));
        backButton.setKeyboard(backRow);
        return backButton;

    }


    public static void main(String[] args) throws TelegramApiException, InterruptedException {
        DataBase db = new DataBase();
        bot = new BotDriver();
        db.start();
        db.join();
        admin = new Admin(bot, db);
        customer = new Customer(bot, db);
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
        telegramBotsApi.registerBot(bot);
    }
}
