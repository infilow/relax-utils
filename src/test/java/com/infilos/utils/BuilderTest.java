package com.infilos.utils;

import org.junit.Test;

import java.util.*;

public class BuilderTest {

    @Test
    public void test() {
        Friend friend = Builder.of(Friend::new)
            .with(Friend::setName, "name")
            .with(Friend::setAge, 22)
            .with(Friend::setVitalStatistics, 33, 44, 55)
            .with(Friend::addHobby, "Study")
            .with(Friend::setBirthday, "2000-01-01")
            .with(Friend::setAddress, "Beijing")
            .with(Friend::setEmail, "name@mail.com")
            .with(Friend::setHairColor, "Gray")
            .with(Friend::addGift, "2000-01-01", "Hat")
            .build();
    }

    private static final class Friend {
        private String name;
        private Integer age;
        private Integer bust;
        private Integer waist;
        private Integer hips;
        private List<String> hobby;
        private String birthday;
        private String address;
        private String mobile;
        private String email;
        private String hairColor;
        private Map<String, String> gifts;

        public void setName(String name) {
            this.name = name;
        }

        public void setAge(Integer age) {
            this.age = age;
        }

        public void setVitalStatistics(int bust, int waist, int hips) {
            this.bust = bust;
            this.waist = waist;
            this.hips = hips;
        }

        public void addHobby(String hobby) {
            this.hobby = Optional.ofNullable(this.hobby).orElse(new ArrayList<>());
            this.hobby.add(hobby);
        }

        public void setBirthday(String birthday) {
            this.birthday = birthday;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public void setMobile(String mobile) {
            this.mobile = mobile;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public void setHairColor(String hairColor) {
            this.hairColor = hairColor;
        }

        public void addGift(String day, String gift) {
            this.gifts = Optional.ofNullable(this.gifts).orElse(new HashMap<>());
            this.gifts.put(day, gift);
        }
    }
}