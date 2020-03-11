package com.sunyt.kafkaPro.page;

import com.alibaba.fastjson.JSON;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class CalTest4 {

    public static class Tree{

        String type;

        List<Tree> typeArgs;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<Tree> getTypeArgs() {
            return typeArgs;
        }

        public void setTypeArgs(List<Tree> typeArgs) {
            this.typeArgs = typeArgs;
        }
    }

    /**
     *
     * @return
     */
    public static void parseMap(String str, Tree rootTree){
        rootTree.setType("Map");
        rootTree.setTypeArgs(new ArrayList<>());

        char []chars = str.toCharArray();
        Stack<Integer> stack = new Stack<>();

        //key value 中间的逗号位置
        int cut = -1;
        int begin = str.indexOf("<");
        for (int i = 0; i < chars.length; i++) {
            char aChar = chars[i];
            //从第一个尖括号开始
            if (aChar == '<'){
                stack.push(1);
            }

            if (aChar == '>'){
                stack.pop();
            }

            // 倒数第二个括号后边是逗号
            if (stack.size() == 1 && aChar == '>'){
                cut = i + 1;
                break;
            }
        }

        String key;
        String value;


        if (cut == -1){
            key = str.substring(str.indexOf("<") + 1, str.indexOf(","));
            value = str.substring(str.indexOf(",") + 1, str.length() - 1);

        } else {
            key = str.substring(begin + 1, cut);
            value = str.substring(cut + 1, str.length() - 1);
        }

        String []args = {key, value};

        for (String arg : args){
            Tree tree = new Tree();
            rootTree.getTypeArgs().add(tree);
            slid(arg, tree);
        }
    }



    public static void pareArray(String str, Tree tree){
        tree.setType("Array");
        tree.setTypeArgs(new ArrayList<>());
        String cutStr = str.substring(str.indexOf("<") + 1, str.length() - 1);
        //新node
        Tree treeTemp = new Tree();
        tree.getTypeArgs().add(treeTemp);
        slid(cutStr, treeTemp);
    }

    public static void slid(String str, Tree tree){
        char s = str.charAt(0);
        switch (s){
            case 'M':
                tree.setType("Map");
                tree.setTypeArgs(new ArrayList<>());
                parseMap(str, tree);
                break;
            case 's':
                //string
                tree.setType("string");
                tree.setTypeArgs(null);
                break;
            case 'A':
                //array
                tree.setType("Array");
                pareArray(str, tree);
                break;
            case 'b':
                tree.setType("bool");
                tree.setTypeArgs(null);
            case 'i':
                tree.setType("int");
                tree.setTypeArgs(null);
        }
    }

    public static void main(String[] args) {
        String test = "Array<Array<Array<int>>>";
        String test2 = "Map<Map<Map<string,bool>,bool>,bool>";
        String tests3 = "Map<Map<string,bool>,bool>";
        String testx4 = "Map<Map<Map<Map<string,string>,bool>,Array<int>>,Map<string,Map<int,bool>>>";
        Tree tree = new Tree();
        slid(test, tree);
        System.out.println(JSON.toJSONString(tree));
    }
}
