package org.funix.lab_6;

public class SongEntity {
    private String name, path, album;

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public String getAlbum() {
        return album;
    }

    public SongEntity(String name, String path, String album) {
        this.name = name;
        this.path = path;
        this.album = album;
    }
}
