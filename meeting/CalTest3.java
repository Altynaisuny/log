package com.sunyt.kafkaPro.page;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CalTest3 {

    public static class Tree{
        String id;

        String type;

        String name;

        List<Tree> children;

        public Tree(String id, String type, String name) {
            this.id = id;
            this.type = type;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<Tree> getChildren() {
            return children;
        }

        public void setChildren(List<Tree> children) {
            this.children = children;
        }
    }
    /**
     * 返回与其匹配的节点
     * @param rootTree 根节点tree
     * @param srcName
     * @return
     */
    public static List<Integer> findNodeById(Tree rootTree, String srcName){
        List<Integer> indexList = new ArrayList<>();
        if (rootTree.getName().startsWith(srcName)){
            indexList.add(getIndex(rootTree.getName()));
        }

        if (!CollectionUtils.isEmpty(rootTree.getChildren())){
            recursion(rootTree.getChildren(), srcName, indexList);
        }

        return indexList;
    }

    /**
     * 截取name 下标后的数字
     * @param name
     * @return
     */
    private static Integer getIndex(String name){
        int index;
        if (name.contains("_")){
            index = Integer.parseInt(name.substring(name.indexOf("_") + 1));
        } else {
            index = 0;
        }
        return index;
    }

    /**
     * 这是一个递归
     * @param rootTree
     * @param srcName
     * @param indexList
     */
    public static void recursion(List<Tree> rootTree, String srcName, List<Integer> indexList){
        for (Tree item : rootTree) {
            if (item.getName().startsWith(srcName)){
                indexList.add(getIndex(item.getName()));
            }
            if (!CollectionUtils.isEmpty(item.getChildren())){
                recursion(item.getChildren(), srcName, indexList);
            }
        }
    }

    /**
     *
     * @param srcName
     * @param rootTreeNode
     * @return
     */
    public static String getIncName(String srcName, Tree rootTreeNode){
        //需要放入哪一种类型的name
        String tempName;
        if (srcName.contains("_")){
            tempName = srcName.substring(0, srcName.indexOf("_"));
        } else {
            tempName = srcName;
        }
        List<Integer> list = findNodeById(rootTreeNode, tempName);
        //先排序
        list.sort(Comparator.naturalOrder());
        int index = -1;
        for (int i = 0; i < list.size() - 1; i++) {
            //序号不是递增的
            if (list.get(i+1) != list.get(i) + 1){
                index = i+1;
                break;
            }
        }
        //序号递增的情况
        if (index == -1){
            index = list.get(list.size() - 1) + 1;
        }
        int indexResult = Integer.parseInt(srcName.substring(srcName.indexOf("_") + 1, srcName.length() - 1) + index);
        if (indexResult == 0){
            return tempName;
        }
        return tempName + "_" + indexResult;

    }

    public static void main(String[] args) {
        Tree view = new Tree("1", "View", "view");
        Tree button = new Tree("2", "Button", "button");
        Tree view1 = new Tree("3", "View", "view_1");
        Tree button1 = new Tree("4", "Button", "button_1");
        Tree view2 = new Tree("5", "View", "view_2");

        List<Tree> treeView1 = new ArrayList<>();
        treeView1.add(button1);
        treeView1.add(view2);
        view1.setChildren(treeView1);

        List<Tree> treeView = new ArrayList<>();
        treeView.add(view1);
        treeView.add(button);
        view.setChildren(treeView);
        String srcName = "view_3";
        System.out.println(getIncName(srcName, view));
    }
}
