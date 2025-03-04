package com.example.TripSpring.dto.domain;

import java.util.Arrays;
import java.util.List;

public enum PlaceCategory {
    CULTURE("문화", Arrays.asList("박물관", "미술관", "공연장")),
    SHOPPING("쇼핑", Arrays.asList("쇼핑몰", "시장", "백화점")),
    FOOD("식사", Arrays.asList("식당", "카페")),
    NATURE("자연", Arrays.asList("공원", "산책로")),
    LANDMARK("랜드마크", Arrays.asList("궁", "타워", "광장")),
    ENTERTAINMENT("엔터테인먼트", Arrays.asList("놀이공원", "영화관"));

    private final String name;
    private final List<String> subCategories;

    PlaceCategory(String name, List<String> subCategories) {
        this.name = name;
        this.subCategories = subCategories;
    }
}