package server.handlers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringInputHandler extends ChannelInboundHandlerAdapter {

    private static final String LS_COMMAND = "ls view all files from current director\n";
    private static final String MKDIR_COMMAND = "mkdir  create directory\n";
    private static final String TOUCH_COMMAND = "touch 'filename' create a file\n";
    private static final String CHANGENICKNAME_COMMAND = "changenickname to change your nickname\n";
    private static final String CD_COMMAND = "cd to change directory\n";
    private static final String RM_COMMAND = "rm to delete file\n";
    private static final String COPY_COMMAND = "copy to copy file\n";
    private static final String CAT_COMMAND = "cat to show file\n";

    Path serverRoot = Paths.get("root");
    //мапа для клиентов
    private final Map<Channel, String> clients = new HashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

        clients.putIfAbsent(ctx.channel(), "user");

        //по совету коллеги сразу сделал массив строк, чтобы убрать разбивку на токены
        String message = String.valueOf(msg);
        String[] cmd = message
                .replace("\n", "")
                .replace("\r", "")
                .split(" ");
        if ("--help".equals(cmd[0])) {
            ctx.write(LS_COMMAND);
            ctx.write(MKDIR_COMMAND);
            ctx.write(TOUCH_COMMAND);
            ctx.write(CD_COMMAND);
            ctx.write(RM_COMMAND);
            ctx.write(COPY_COMMAND);
            ctx.write(CAT_COMMAND);
            ctx.write(CHANGENICKNAME_COMMAND);


        } else if ("ls".equals(cmd[0])) {
            ctx.write(getFilesList().concat("\n"));
        } else if ("touch".equals(cmd[0])) {
            ctx.write(createFile(serverRoot, cmd[1]));
        } else if ("changenickname".equals(cmd[0])) {
            ctx.write(changeNick(cmd[1], ctx.channel()));
        } else if ("mkdir".equals(cmd[0])) {
            ctx.write(createDirectory(cmd[1], serverRoot));
        } else if ("cd".equals(cmd[0])) {
            ctx.write(changeDirectory(cmd[1]));
        } else if ("~".equals(cmd[0])) {
            ctx.write(changeDirectoryToRoot());
        } else if ("..".equals(cmd[0])) {
            ctx.write(changeDirectoryUp());
        } else if ("rm".equals(cmd[0])) {
            ctx.write(deleteFile(cmd[1], serverRoot));
        } else if ("copy".equals(cmd[0])) {
            ctx.write(copyFile(cmd[1], serverRoot, cmd[2]));
        } else if ("cat".equals(cmd[0])) {
            ctx.write(readFile(cmd[1], serverRoot));
        }
    }

    //делал на 1.8, так что конструкция File.of была заменена
    //чтение
    private Object readFile(String fileName, Path serverRoot) throws IOException {
        List<String> textFile = Files.readAllLines(Paths.get(String.valueOf(serverRoot), fileName), StandardCharsets.UTF_8);
        for (String s : textFile) {
            return s;
        }
        return "\n";
    }

    //копирование
    private Object copyFile(String fileName, Path serverRoot, String target) throws IOException {
        Files.copy((Paths.get(String.valueOf(serverRoot), fileName)), (Paths.get(String.valueOf(serverRoot), target)));
        return "File/Directory " + fileName + " copy in " + target;
    }


    //удаление файла или директории, работает корректно, даже если в директории есть файлы
    //плохо работает проверка на наличие файла, клиент крашится
    private Object deleteFile(String fileName, Path serverRoot) throws IOException {
        Path pathRM = Paths.get(serverRoot + "/" + fileName);
        Files.walkFileTree(pathRM, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (Files.exists(Paths.get(String.valueOf(serverRoot)))) {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }  else {
                    System.out.println("Directory or file " + file.getFileName().toString() + " doesn't exists\n");
                    return FileVisitResult.CONTINUE;
                }
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
        return "File/directory " + fileName + " deleted.\n";
    }

    //смена директории вверх
    private Object changeDirectoryUp() {
        serverRoot = serverRoot.getParent();
        return "change directory " + serverRoot;
    }

    //смена директории на корневой каталог
    private Object changeDirectoryToRoot() {
        serverRoot = Paths.get("root");
        return "change directory " + serverRoot;
    }

    //смена директории вглубь
    private Object changeDirectory(String change) {
        if (Files.exists(Paths.get(String.valueOf(serverRoot), change))) {
            serverRoot = Paths.get(String.valueOf(serverRoot), change);
            return "change directory " + serverRoot;
        } else {
            return "Directory " + change + " doesn't exists\n";
        }
    }

    //создание директории
    private Object createDirectory(String dir, Path serverRoot) throws IOException {
        if (!Files.exists(Paths.get(String.valueOf(serverRoot), dir))) {
            Files.createDirectory(Paths.get(String.valueOf(serverRoot), dir));
            return "Directory " + dir + " created.\n";
        } else {
            return "Directory " + dir + " already exists\n";
        }
    }

    //смена ника
    private Object changeNick(String newNick, Channel client) {
        clients.replaceAll((k, v) -> newNick);
        return "New nickname " + newNick;
    }

    // touch (filename) - создание файла
    //коммент для пуллреквеста, ниже метод
    //метод упрощён и переделан
    private Object createFile(Path serverRoot, String fileName) throws IOException {
        if (!Files.exists(Paths.get(String.valueOf(serverRoot), fileName))) {
            Files.createFile(Paths.get(String.valueOf(serverRoot), fileName));
            return "File " + fileName + " created.\n";
        } else {
            return "File " + fileName + " already exists\n";
        }
    }

    //корректный вывод файлов в текущем каталоге
    private String getFilesList() {
        String[] servers = new File(String.valueOf(Paths.get(String.valueOf(serverRoot)))).list();
        return String.join(" ", servers);
    }
}
