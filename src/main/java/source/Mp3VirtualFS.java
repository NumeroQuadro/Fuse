package source;

import jnr.ffi.Pointer;
import ru.serce.jnrfuse.AbstractFuseFS;
import ru.serce.jnrfuse.ErrorCodes;
import ru.serce.jnrfuse.FuseFillDir;
import ru.serce.jnrfuse.struct.FileStat;
import ru.serce.jnrfuse.struct.FuseFileInfo;
import ru.serce.jnrfuse.struct.FusePollhandle;
import ru.serce.jnrfuse.struct.Timespec;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Mp3VirtualFS extends AbstractFuseFS {
    private final Path sourceDir;
    private final Map<String, Set<Path>> groupedByArtist;
    private final Map<String, Set<Path>> groupedByGenre;
    private final Map<String, Set<Path>> groupedByYear;

    public Mp3VirtualFS(Path sourceDir) {
        this.sourceDir = sourceDir;
        Mp3Grouper grouper = new Mp3Grouper(sourceDir);
        grouper.groupFiles();
        this.groupedByArtist = grouper.getGroupedByArtist();
        this.groupedByGenre = grouper.getGroupedByGenre();
        this.groupedByYear = grouper.getGroupedByYear();
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Usage: Mp3VirtualFS <source_dir> <mount_point>");
            System.exit(1);
        }

        Path mp3Dir = Paths.get(args[0]);
        Path mountPoint = Paths.get(args[1]);

        if (!Files.isDirectory(mp3Dir)) {
            System.err.println("The source directory is not valid: " + mp3Dir);
            System.exit(1);
        }

        Mp3VirtualFS fs = new Mp3VirtualFS(mp3Dir);
        fs.mount(mountPoint, true, false);
    }

    @Override
    public int getattr(String path, FileStat stat) {
        if ("/".equals(path)) {
            stat.st_mode.set(FileStat.S_IFDIR | 0755);
            stat.st_nlink.set(2);
            return 0;
        }

        if (groupedByArtist.containsKey(path) || groupedByGenre.containsKey(path) || groupedByYear.containsKey(path)) {
            stat.st_mode.set(FileStat.S_IFDIR | 0755);
            stat.st_nlink.set(2);
            return 0;
        }

        Path filePath = sourceDir.resolve(path.substring(1));
        if (Files.exists(filePath)) {
            stat.st_mode.set(FileStat.S_IFREG | 0644);
            stat.st_nlink.set(1);
            stat.st_size.set(filePath.toFile().length());
            return 0;
        }

        return -ErrorCodes.ENOENT();
    }

    @Override
    public int readdir(String path, Pointer buf, FuseFillDir filler, long offset, FuseFileInfo fi) {
        filler.apply(buf, ".", null, 0);
        filler.apply(buf, "..", null, 0);

        if ("/".equals(path)) {
            groupedByArtist.keySet().forEach(artist -> filler.apply(buf, "Artist/" + artist, null, 0));
            groupedByGenre.keySet().forEach(genre -> filler.apply(buf, "Genre/" + genre, null, 0));
            groupedByYear.keySet().forEach(year -> filler.apply(buf, "Year/" + year, null, 0));
            return 0;
        }

        if (path.startsWith("/Artist/")) {
            String artist = path.substring("/Artist/".length());
            if (groupedByArtist.containsKey(artist)) {
                groupedByArtist.get(artist).forEach(file -> filler.apply(buf, file.getFileName().toString(), null, 0));
                return 0;
            }
        } else if (path.startsWith("/Genre/")) {
            String genre = path.substring("/Genre/".length());
            if (groupedByGenre.containsKey(genre)) {
                groupedByGenre.get(genre).forEach(file -> filler.apply(buf, file.getFileName().toString(), null, 0));
                return 0;
            }
        } else if (path.startsWith("/Year/")) {
            String year = path.substring("/Year/".length());
            if (groupedByYear.containsKey(year)) {
                groupedByYear.get(year).forEach(file -> filler.apply(buf, file.getFileName().toString(), null, 0));
                return 0;
            }
        }

        return -ErrorCodes.ENOENT();
    }

    @Override
    public int open(String path, FuseFileInfo fi) {
        Path filePath = sourceDir.resolve(path.substring(1));
        if (Files.exists(filePath)) {
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int read(String path, Pointer buf, long size, long offset, FuseFileInfo fi) {
        Path filePath = sourceDir.resolve(path.substring(1));
        try {
            byte[] data = Files.readAllBytes(filePath);
            int length = (int) Math.min(data.length - offset, size);
            buf.put(0, data, (int) offset, length);
            return length;
        } catch (IOException e) {
            e.printStackTrace();
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int mkdir(String path, long mode) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int rmdir(String path) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int unlink(String path) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int symlink(String oldpath, String newpath) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int rename(String oldpath, String newpath) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int link(String oldpath, String newpath) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int chmod(String path, long mode) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int chown(String path, long uid, long gid) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int truncate(String path, long size) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int write(String path, Pointer buf, long size, long offset, FuseFileInfo fi) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int create(String path, long mode, FuseFileInfo fi) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int ftruncate(String path, long size, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int utimens(String path, Timespec[] timespecs) {
        return 0;
    }

    @Override
    public int access(String path, int mask) {
        return 0;
    }

    @Override
    public int flush(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int release(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int statfs(String path, ru.serce.jnrfuse.struct.Statvfs stbuf) {
        try {
            Path filePath = sourceDir.resolve(path.substring(1));
            long totalSpace = Files.getFileStore(filePath).getTotalSpace();
            long freeSpace = Files.getFileStore(filePath).getUsableSpace();
            long blockSize = Files.getFileStore(filePath).getBlockSize();

            stbuf.f_blocks.set(totalSpace / blockSize);
            stbuf.f_bfree.set(freeSpace / blockSize);
            stbuf.f_bavail.set(freeSpace / blockSize);
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return -ErrorCodes.EIO();
        }
    }

    @Override
    public int readlink(String path, Pointer buf, long size) {
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int mknod(String path, long mode, long rdev) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int fgetattr(String path, FileStat stat, FuseFileInfo fi) {
        return getattr(path, stat);
    }

    @Override
    public int fsync(String path, int isdatasync, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int setxattr(String path, String name, Pointer value, long size, int flags) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int getxattr(String path, String name, Pointer value, long size) {
        return -ErrorCodes.ENOATTR();
    }

    @Override
    public int listxattr(String path, Pointer list, long size) {
        return 0;
    }

    @Override
    public int removexattr(String path, String name) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int opendir(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int releasedir(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int fsyncdir(String path, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public Pointer init(Pointer conn) {
        return null;
    }

    @Override
    public void destroy(Pointer conn) {
    }

    @Override
    public int lock(String path, FuseFileInfo fi, int cmd, ru.serce.jnrfuse.struct.Flock flock) {
        return 0;
    }

    @Override
    public int bmap(String path, long blocksize, long idx) {
        return 0;
    }

    @Override
    public int ioctl(String path, int cmd, Pointer arg, FuseFileInfo fi, long flags, Pointer data) {
        return 0;
    }

    @Override
    public int poll(String path, FuseFileInfo fi, FusePollhandle ph, Pointer reventsp) {
        return 0;
    }

    @Override
    public int write_buf(String path, ru.serce.jnrfuse.struct.FuseBufvec buf, long off, FuseFileInfo fi) {
        return -ErrorCodes.EROFS();
    }

    @Override
    public int read_buf(String path, Pointer bufp, long size, long off, FuseFileInfo fi) {
        return 0;
    }

    @Override
    public int flock(String path, FuseFileInfo fi, int op) {
        return 0;
    }

    @Override
    public int fallocate(String path, int mode, long off, long len, FuseFileInfo fi) {
        return -ErrorCodes.EROFS();
    }
}
