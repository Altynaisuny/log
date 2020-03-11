package com.sunyt.kafkaPro.page;

import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 笔试题目0
 * TreeNode 查询
 */
public class CalTest2 {

    public static class Tree{
        String id;
        String label;
        List<Tree> children;

        public Tree(String id, String label) {
            this.id = id;
            this.label = label;
        }

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
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
     * @param id id
     * @return
     */
    public static List<Tree> findNodeById(Tree rootTree, String id){
        List<Tree> result = new ArrayList<>();

        if (rootTree.getId().equals(id)){
            result.add(rootTree);
        }


        if (!CollectionUtils.isEmpty(rootTree.getChildren())){
            recursion(rootTree.getChildren(), id, result);
        }

        return result;
    }

    /**
     * 这是一个递归
     * @param rootTree
     * @param id
     * @param result
     */
    public static void recursion(List<Tree> rootTree, String id, List<Tree> result){
        for (Tree item : rootTree) {
            if (item.getId().equals(id)){
                //todo 此处仅筛选，不做reference置空处理。
                result.add(item);
            }
            if (!CollectionUtils.isEmpty(item.getChildren())){
                recursion(item.getChildren(), id, result);
            }
        }
    }

    public static void main(String[] args) {
        // 先放入模拟数据
        Tree rootTree = new Tree("1", "first");

        Tree treeSec = new Tree("2", "second");
        Tree treeThree = new Tree("3", "third");

        Tree TreeFour = new Tree("4", "forth");
        Tree TreeFive = new Tree("5", "fifth");

        List<Tree> thirdList = new ArrayList<>();
        thirdList.add(TreeFour);
        thirdList.add(TreeFive);
        treeThree.setChildren(thirdList);

        List<Tree> twoList = new ArrayList<>();
        twoList.add(treeSec);
        twoList.add(treeThree);

        rootTree.setChildren(twoList);


        //查找
        List<Tree> result = CalTest2.findNodeById(rootTree, "2");
        result.forEach(item->{

            System.out.println(item.getLabel());
        });
    }

}
