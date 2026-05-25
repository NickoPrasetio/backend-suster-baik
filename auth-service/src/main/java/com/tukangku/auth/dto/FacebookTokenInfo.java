package com.tukangku.auth.dto;

import lombok.Data;

/**
 * Mapping respons dari Facebook Graph API:
 * GET https://graph.facebook.com/me?fields=id,name,email,picture.type(large)
 *     &access_token={access_token}
 *
 * Catatan: email bisa null jika user Facebook tidak punya email terhubung
 * atau tidak mengizinkan permission 'email'.
 */
@Data
public class FacebookTokenInfo {

    /** Facebook user ID — unik per user per app */
    private String id;

    private String name;

    /** Bisa null jika tidak diizinkan / tidak ada email di akun FB */
    private String email;

    /** Nested object: { "data": { "url": "..." } } */
    private PictureWrapper picture;

    /** Ambil URL avatar, null jika tidak ada */
    public String getPictureUrl() {
        if (picture == null) return null;
        if (picture.getData() == null) return null;
        return picture.getData().getUrl();
    }

    @Data
    public static class PictureWrapper {
        private PictureData data;
    }

    @Data
    public static class PictureData {
        private String url;
        private Boolean isSilhouette;
    }
}
