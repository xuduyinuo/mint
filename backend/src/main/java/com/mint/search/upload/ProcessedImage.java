package com.mint.search.upload;

public record ProcessedImage(byte[] bytes, String fileName, String contentType) {
    public long size() {
        return bytes.length;
    }
}
