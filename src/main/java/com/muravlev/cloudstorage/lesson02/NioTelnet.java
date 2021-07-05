package com.muravlev.cloudstorage.lesson02;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class NioTelnet {
    private static final String LS_COMMAND = "ls view all files from current director\n";
    private static final String MKDIR_COMMAND = "mkdir  create directory\n";
    private static final String TOUCH_COMMAND = "touch 'filename' create a file\n";
    private static final String CHANGENICKNAME_COMMAND = "changenickname to change your nickname\n";
    private static final String CD_COMMAND = "cd to change directory\n";
    private static final String RM_COMMAND = "rm to delete file\n";
    private static final String COPY_COMMAND = "copy to copy file\n";
    private static final String CAT_COMMAND = "cat to show file\n";

    Path serverRoot = Path.of("root");

    private final ByteBuffer buffer = ByteBuffer.allocate(512);

    private Map<SocketChannel, String> clients = new HashMap<SocketChannel, String>();


    public NioTelnet() throws Exception {
        ServerSocketChannel server = ServerSocketChannel.open();
        server.bind(new InetSocketAddress(5679));
        server.configureBlocking(false);
        Selector selector = Selector.open();

        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");
        while (server.isOpen()) {
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            Iterator<SelectionKey> iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client connected. IP:" + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "skjghksdhg");
        channel.write(ByteBuffer.wrap(("Hello " + channel.getRemoteAddress().toString() + "\n").getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter --help for support info\n".getBytes(StandardCharsets.UTF_8)));
    }


    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress client = channel.getRemoteAddress();
        int readBytes = channel.read(buffer);

        clients.putIfAbsent(channel, "user");

        if (readBytes < 0) {
            channel.close();
            return;
        } else if (readBytes == 0) {
            return;
        }

        buffer.flip();
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }
        buffer.clear();

        // TODO: 21.06.2021
        // touch (filename) - создание файла                      - сделано
        // mkdir (dirname) - создание директории                  - сделано
        // cd (path | ~ | ..) - изменение текущего положения      - сделано (без ~, ..) прошу время доработать
        // rm (filename / dirname) - удаление файла / директории  - сделано
        // copy (src) (target) - копирование файлов / директории
        // cat (filename) - вывод содержимого текстового файла
        // changenick (nickname) - изменение имени пользователя   - сделано
        // добавить имя клиента                                   - сделано

        // добавить имя клиента

        if (key.isValid()) {
            String command = sb.toString()
                    .replace("\n", "")
                    .replace("\r", "");
            if ("--help".equals(command)) {
                sendMessage(LS_COMMAND, selector, client);
                sendMessage(MKDIR_COMMAND, selector, client);
                sendMessage(TOUCH_COMMAND, selector, client);
                sendMessage(CD_COMMAND, selector, client);
                sendMessage(RM_COMMAND, selector, client);
                sendMessage(COPY_COMMAND, selector, client);
                sendMessage(CAT_COMMAND, selector, client);


                //добавил корректные выводы
            } else if ("ls".equals(command)) {
                //channel.write(ByteBuffer.wrap((clients.get(channel.getRemoteAddress()) + "~" + serverRoot + ": \n").getBytes(StandardCharsets.UTF_8)));
                sendMessage(getFilesList().concat("\n"), selector, client);
                //коммент для пуллреквеста, ниже команда
            } else if (command.startsWith("touch")) {
                String[] tokens = command.split(" ");
                sendMessage(createFile(serverRoot, tokens[1]), selector, client);
            } else if (command.startsWith("changenickname")) {
                String[] tokens = command.split(" ");
                sendMessage(changeNick(client, tokens[1]), selector, client);
            } else if (command.startsWith("mkdir")) {
                String[] tokens = command.split(" ");
                sendMessage(createDirectory(tokens[1], serverRoot), selector, client);
            } else if (command.startsWith("cd")) {
                String[] tokens = command.split(" ");
                sendMessage(changeDirectory(tokens[1]), selector, client);
            } else if (command.startsWith("~")) {
                sendMessage(changeDirectoryToRoot(), selector, client);
            } else if (command.startsWith("..")) {
                sendMessage(changeDirectoryUp(), selector, client);
            } else if (command.startsWith("rm")) {
                String[] tokens = command.split(" ");
                sendMessage(deleteFile(tokens[1], serverRoot), selector, client);
            } else if (command.startsWith("copy")) {
                String[] tokens = command.split(" ");
                sendMessage(copyFile(tokens[1], serverRoot, tokens[2]), selector, client);
            } else if (command.startsWith("cat")) {
                String[] tokens = command.split(" ");
                sendMessage(readFile(tokens[1], serverRoot), selector, client);
            }

        }
    }

    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                if (((SocketChannel) key.channel()).getRemoteAddress().equals(client)) {
                    ((SocketChannel) key.channel()).write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }

    //корректный вывод файлов в текущем каталоге
    private String getFilesList() {
        String[] servers = new File(String.valueOf(Path.of(String.valueOf(serverRoot)))).list();
        return String.join(" ", servers);
    }

    // touch (filename) - создание файла
    //коммент для пуллреквеста, ниже метод
    //метод упрощён и переделан
    private String createFile(Path serverRoot, String fileName) throws IOException {
        if (!Files.exists(Path.of(String.valueOf(serverRoot), fileName))) {
            Files.createFile(Path.of(String.valueOf(serverRoot), fileName));
            return "File " + fileName + " created.\n";
        } else {
            return "File " + fileName + " already exists\n";
        }
    }

    // mkdir (dirname) - создание директории
    //коммент для пуллреквеста, ниже метод
    //метод упрощён и переделан
    private String createDirectory(String dir, Path serverRoot) throws IOException {
        if (!Files.exists(Path.of(String.valueOf(serverRoot), dir))) {
            Files.createDirectory(Path.of(String.valueOf(serverRoot), dir));
            return "Directory " + dir + " created.\n";
        } else {
            return "Directory " + dir + " already exists\n";
        }
    }

    // changenick (nickname) - изменение имени пользователя
    // добавить имя клиента
    //коммент для пуллреквеста, ниже метод
    //при коннекте мы присваиваем ник клиенту, который равен строковому представлению его адреса
    //и заносим его в мапу... метод ниже позволяет сменить ник

    //Новый метод
    //метод упрощён и переделан
    public String changeNick(SocketAddress client, String newNick) throws IOException {
        clients.replaceAll((k, v) -> newNick);
        return "New nickname " + newNick;
    }

    // cd (path | ~ | ..) - изменение текущего положения
    //работает криво, прошу время доработать
    //доработал метод, реализация ~ и .. ниже в отдельных методах
    private String changeDirectory(String change) {
        if (Files.exists(Path.of(String.valueOf(serverRoot), change))) {
            serverRoot = Path.of(String.valueOf(serverRoot), change);
            return "change directory " + serverRoot;
        } else {
            return "Directory " + change + " doesn't exists\n";
        }
    }

    //сделал отдельные методы для возвращение в рут и на одну папку
    private String changeDirectoryToRoot() {
        serverRoot = Path.of("root");
        return "change directory " + serverRoot;
    }

    private String changeDirectoryUp() {
        serverRoot = serverRoot.getParent();
        return "change directory " + serverRoot;
    }

    // rm (filename / dirname) - удаление файла / директории
    // сделал через walkFileTree, работает более ли менее корректно
    private String deleteFile(String fileName, Path serverRoot) throws IOException {
        Path pathRM = Path.of(serverRoot + "/" + fileName);
        Files.walkFileTree(pathRM, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        return "File/directory " + fileName + " deleted.\n";
    }


    //исправлено
    private String copyFile(String fileName, Path serverRoot, String target) throws IOException {
        Files.copy((Path.of(String.valueOf(serverRoot), fileName)), (Path.of(String.valueOf(serverRoot), target)));
        return "File/Directory " + fileName + " copy in " + target;
    }


    // cat (filename) - вывод содержимого текстового файла
    //исправлено
    private String readFile(String fileName, Path serverRoot) throws IOException {
        List<String> textFile = Files.readAllLines(Path.of(String.valueOf(serverRoot), fileName), StandardCharsets.UTF_8);
        for (String s : textFile) {
            return s;
        }
        return "\n";

    }

    public static void main(String[] args) throws Exception {
        new NioTelnet();
    }
}
