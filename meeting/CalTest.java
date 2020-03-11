package com.sunyt.kafkaPro.page;

import com.alibaba.fastjson.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 笔试题目1
 */
public class CalTest {

    public static class Student{


        public Student(String name, Integer score){
            this.name = name;
            this.score = score;
        }

        String name;

        Integer score;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }
    }

    public static String getGrade(Integer score){
        return score < 60 ? "C": score < 80 ? "B" : "A";
    }

    /**
     * key :grade
     * value List<student>
     * @param students
     * @return
     */
    public Map<String, List<Student>> groupBy(List<Student> students){
        Map<String, List<Student>> result = new HashMap<>();
        students.forEach(item->{
            String score = CalTest.getGrade(item.getScore());
            if (result.containsKey(score)){
                result.get(score).add(item);
            } else {
                List<Student> list = new ArrayList<>();
                list.add(item);
                result.put(score, list);
            }
        });
        return result;

    }

    public static void main(String[] args) {

        List<Student> students = new ArrayList<>();
        students.add(new Student("张三", 84));
        students.add(new Student("李四", 58));
        students.add(new Student("王五", 99));
        students.add(new Student("赵六", 69));


        System.out.println(JSONObject.toJSON(new CalTest().groupBy(students)));


    }


}
