package com.muravlev.cloudstorage.lesson02;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class NioTelnetServer {
    private static final String LS_COMMAND = "ls view all files from current director\n";
    private static final String MKDIR_COMMAND = "mkdir  create directory\n";
    private static final String TOUCH_COMMAND = "touch 'filename' create a file\n";
    private static final String CHANGENICKNAME_COMMAND = "changenickname to change your nickname\n";
    private static final String CD_COMMAND = "cd to change directory\n";
    private static final String RM_COMMAND = "rm to delete file\n";
    private static final String COPY_COMMAND = "copy to copy file\n";
    private static final String CAT_COMMAND = "cat to show file\n";

    private final ByteBuffer buffer = ByteBuffer.allocate(512);

    private Map<SocketAddress, String> clients = new HashMap<>();

    //мапа для получения директории клиента
    private Map<SocketAddress, Path> clientpath = new HashMap<>();

    String root = "root";
    Path path = Path.of(root);

    public NioTelnetServer() throws Exception {
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

    public void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client connected. IP:" + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "skjghksdhg");
        channel.write(ByteBuffer.wrap(("Hello " + channel.getRemoteAddress().toString() + "\n").getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter --help for support info\n".getBytes(StandardCharsets.UTF_8)));

        // Path clientfirstpath = Path.of("root", clients.get(channel.getRemoteAddress()));

        //просто к примеру вручную присваиваем пользователю какой-то начальный ник
        //хотел сделать начальным ником его адрес, но символ : не подходит для этого
        clients.put(channel.getRemoteAddress(), "default_user");

        //создаём директорию для пользователя
        if (!Files.exists(Path.of("root", clients.get(channel.getRemoteAddress())))) {
            Files.createDirectory(Path.of("root", clients.get(channel.getRemoteAddress())));
        }

        //помещаем в мапу clientpath путь для текущего юзера (делаю для одного клиента)
        clientpath.put(channel.getRemoteAddress(), Path.of("root", clients.get(channel.getRemoteAddress())));
        //channel.write(ByteBuffer.wrap(("Currpath: " +(Path.of("root", clients.get(channel.getRemoteAddress())))).getBytes(StandardCharsets.UTF_8)));
    }


    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress client = channel.getRemoteAddress();
        int readBytes = channel.read(buffer);

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

        if (key.isValid()) {
            String command = sb.toString()
                    .replace("\n", "")
                    .replace("\r", "");
            if ("--help".equals(command)) {
                channel.write(ByteBuffer.wrap((clients.get(channel.getRemoteAddress()) + "~" + clientpath.get(channel.getRemoteAddress()) + ": \n").getBytes(StandardCharsets.UTF_8)));
                sendMessage(LS_COMMAND, selector, client);
                sendMessage(MKDIR_COMMAND, selector, client);
                sendMessage(TOUCH_COMMAND, selector, client);
                sendMessage(CD_COMMAND, selector, client);
                sendMessage(RM_COMMAND, selector, client);
                sendMessage(COPY_COMMAND, selector, client);
                sendMessage(CAT_COMMAND, selector, client);

            } else if ("ls".equals(command)) {
                channel.write(ByteBuffer.wrap((clients.get(channel.getRemoteAddress()) + "~" + clientpath.get(channel.getRemoteAddress()) + ": \n").getBytes(StandardCharsets.UTF_8)));
                sendMessage(getFilesList().concat("\n"), selector, client);
                //коммент для пуллреквеста, ниже команда
            } else if (command.startsWith("touch")) {
                channel.write(ByteBuffer.wrap((clients.get(channel.getRemoteAddress()) + "~" + clientpath.get(channel.getRemoteAddress()) + ": \n").getBytes(StandardCharsets.UTF_8)));
                //строку на токены
                String[] tokens = command.split(" ");
                createFile(key, tokens[1]);
            } else if (command.startsWith("changenickname")) {
                channel.write(ByteBuffer.wrap((clients.get(channel.getRemoteAddress()) + "~" + clientpath.get(channel.getRemoteAddress()) + ": \n").getBytes(StandardCharsets.UTF_8)));
                String[] tokens = command.split(" ");
                changeNick(key, tokens[1]);
            } else if (command.startsWith("mkdir")) {
                channel.write(ByteBuffer.wrap((clients.get(channel.getRemoteAddress()) + "~" + clientpath.get(channel.getRemoteAddress()) + ": \n").getBytes(StandardCharsets.UTF_8)));
                String[] tokens = command.split(" ");
                createDirectory(key, tokens[1]);
            } else if (command.startsWith("cd")) {
                channel.write(ByteBuffer.wrap((clients.get(channel.getRemoteAddress()) + "~" + clientpath.get(channel.getRemoteAddress()) + ": \n").getBytes(StandardCharsets.UTF_8)));
                String[] tokens = command.split(" ");
                changeDirectory(key, Path.of(tokens[1]));
            } else if (command.startsWith("rm")) {
                String[] tokens = command.split(" ");
                deleteFile(tokens[1]);
            } else if (command.startsWith("copy")) {
                channel.write(ByteBuffer.wrap((clients.get(channel.getRemoteAddress()) + "~" + clientpath.get(channel.getRemoteAddress()) + ": \n").getBytes(StandardCharsets.UTF_8)));
                String[] tokens = command.split(" ");
                copyFile(tokens[1], Path.of(tokens[2]));
            } else if (command.startsWith("cat")) {
                channel.write(ByteBuffer.wrap((clients.get(channel.getRemoteAddress()) + "~" + clientpath.get(channel.getRemoteAddress()) + ": \n").getBytes(StandardCharsets.UTF_8)));
                String[] tokens = command.split(" ");
                readFile(Path.of(tokens[1]));
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

    private String getFilesList() {
        String[] servers = new File("server").list();
        return String.join(" ", servers);
    }


    // touch (filename) - создание файла
    //коммент для пуллреквеста, ниже метод
    private void createFile(SelectionKey key, String filename) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        Path path1 = Path.of("root", clients.get(channel.getRemoteAddress()), filename);
        if (!Files.exists(path1)) {
            try {
                Files.createFile(path1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // mkdir (dirname) - создание директории
    //коммент для пуллреквеста, ниже метод
    private void createDirectory(SelectionKey key, String directoryname) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        Files.createDirectories(Path.of("root", clients.get(channel.getRemoteAddress()), directoryname));
    }

    // changenick (nickname) - изменение имени пользователя
    // добавить имя клиента
    //коммент для пуллреквеста, ниже метод
    //при коннекте мы присваиваем ник клиенту, который равен строковому представлению его адреса
    //и заносим его в мапу... метод ниже позволяет сменить ник
    public void changeNick(SelectionKey key, String newNick) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress client = channel.getRemoteAddress();
        channel.write(ByteBuffer.wrap(("Old nickname: " + clients.get(client) + "\n").getBytes(StandardCharsets.UTF_8)));
        clients.put(client, newNick);
        channel.write(ByteBuffer.wrap(("New nickname: " + clients.get(client) + "\n").getBytes(StandardCharsets.UTF_8)));
    }

    // cd (path | ~ | ..) - изменение текущего положения
    //работает криво, прошу время доработать
    private String changeDirectory(SelectionKey key, Path newpath) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        Path currentpath = clientpath.get(channel.getRemoteAddress());
        Path path = Paths.get(clientpath.get(channel.getRemoteAddress()) + File.separator + newpath).normalize();
        if (Files.exists(path)) {
            clientpath.put(channel.getRemoteAddress(), path);
            channel.write(ByteBuffer.wrap(("Current path: " + clientpath.get(channel.getRemoteAddress()).toString()).getBytes(StandardCharsets.UTF_8)));
        } else {
            return "ERROR: The path does not exist!\n\r";
        }
        return path.toString();
    }

    // rm (filename / dirname) - удаление файла / директории
    // сделал через walkFileTree, работает более ли менее корректно
    private void deleteFile(String filename) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (filename.equals(file.getFileName().toString())) {
                    Files.delete(file.toAbsolutePath());
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                if (filename.equals(dir.getFileName().toString())) {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }


    // copy (src) (target) - копирование файлов / директории
    // не работает, знаю, как исправить, дело в том, где он ищет файл
    private void copyFile(String filename, Path to) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (filename.equals(file.getFileName().toString())) {
                    //Files.copy(filename, to.resolve(from.relativize(filename)));
                    Files.copy(Path.of(filename), to);
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }


    // cat (filename) - вывод содержимого текстового файла
    private void readFile(Path file) throws IOException {
        FileChannel channel = new RandomAccessFile("root" + File.separator + file.getFileName().toString(), "rw").getChannel();
        ByteBuffer buffer = ByteBuffer.allocate(1000);
        channel.read(buffer);
        buffer.flip();

        byte[] byteBuf = new byte [1000];
        int pos = 0;
        while (buffer.hasRemaining()) {
            byteBuf[pos++] = buffer.get();
        }
        channel.write(ByteBuffer.wrap((new String(byteBuf, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8))));

        buffer.rewind();
    }


    public static void main(String[] args) throws Exception {
        new NioTelnetServer();
    }
}
