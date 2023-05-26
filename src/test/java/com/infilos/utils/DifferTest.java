package com.infilos.utils;

import com.infilos.utils.differ.FieldInfo;
import org.junit.Test;

import java.util.*;

public class DifferTest {
    
    public static class Person {
        private final String name;
        private final Integer age;

        public Person(String name, Integer age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() {
            return name;
        }
        
        public Integer getAge() {
            return age;
        }
    }

    @Test
    public void test() {
        String val1 = "val1";
        String val2 = "val2";
        List<Integer> list1 = java.util.Arrays.asList(1,2);
        List<Integer> list2 = java.util.Arrays.asList(3,4);
        Person person1 = new Person("Anna", 22);
        Person person2 = new Person("Luna", 23);
        
        Differ fieldDiffer = Differ.builder()
            .include()
            .exclude()
            .onlyBothExist(true)
            .baseField();

        Differ getterDiffer = Differ.builder()
            .include()
            .exclude()
            .onlyBothExist(true)
            .baseGetter();
        
        List<FieldInfo> diff1 = fieldDiffer.diff(val1, val2); 
        List<FieldInfo> diff2 = fieldDiffer.diff(list1, list2); 
        List<FieldInfo> diff3 = fieldDiffer.diff(person1, person2); 
        List<FieldInfo> diff4 = getterDiffer.diff(person1, person2); 

        System.out.println(diff1);
        System.out.println(diff2);
        System.out.println(diff3);
        System.out.println(diff4);
    }
}