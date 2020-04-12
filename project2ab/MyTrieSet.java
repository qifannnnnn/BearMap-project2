package bearmaps.proj2ab;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class MyTrieSet{
    Node root;
    int size;

    public class DataIndexCharMap<T> {
        T[] items;

        public DataIndexCharMap(){
            items=(T[]) new Object[128];
        }

        public T get(int i){
            return items[i];
        }

        public void put(int i, T thing){
            items[i]=thing;
        }
    }

    private class Node {
        Character value;
        boolean isKey;
        DataIndexCharMap<Node> next;

        public Node(boolean key) {
            value=null;
            isKey = key;
            next = new DataIndexCharMap<Node>();
        }
    }

    public MyTrieSet() {
        root = new Node(false);
        size = 0;
    }

    public void clear() {
        root = new Node(false);
        size = 0;
    }


    public boolean contains(String key) {
        return contains(key, root);
    }

    private boolean contains(String key, Node n) {
        Character target = key.charAt(0);
        if (n.next.get(target.hashCode()) == null) {
            return false;
        }
        if (key.length() == 1) {
            if (n.next.get(target.hashCode()).isKey == false) {
                return false;
            } else {
                return true;
            }
        }
        return contains(key.substring(1), n.next.get(target.hashCode()));

    }

    public void add(String key) {
        if (contains(key)) {
            return;
        }
        size++;
        add(key, root);
    }

    private void add(String key, Node n) {
        // the first value of the string
        Character target = key.charAt(0);
        // if no value is here
        if (n.next.get(target.hashCode()) == null) {
            if (key.length() == 1) {
                Node tempNode = new Node(true);
                n.next.put(target.hashCode(), tempNode);
                n.next.get(target.hashCode()).value=target;
                return;
            } else {
                Node tempNode = new Node(false);
                n.next.put(target.hashCode(), tempNode);
                add(key.substring(1), n.next.get(target.hashCode()));
                n.next.get(target.hashCode()).value=target;
                return;
            }
        } else {
            if (key.length() == 1) {
                n.next.get(target.hashCode()).isKey = true;
                return;
            } else {
                add(key.substring(1), n.next.get(target.hashCode()));
                n.next.get(target.hashCode()).value=target;
                return;
            }
        }

    }

    private List<String> collectKeys(String s, Node n) {
        List<String> ls=new ArrayList<>();
        if (contains(s)) {
            ls.add(s);
        }
        collectHelper(s,n,ls);
        return ls;
    }

    private void collectHelper(String s, Node n, List<String> ls){
        for (int i=0;i<=127;i++){
            if (n.next.get(i)==null) {continue; }
            else {
                if (n.next.get(i).isKey) {
                    ls.add(s+n.next.get(i).value);
                    collectHelper(s+n.next.get(i).value, n.next.get(i),ls);
                }
                else
                    collectHelper(s+n.next.get(i).value,n.next.get(i),ls);
            }
        }
        return;
    }


    public List<String> keysWithPrefix(String prefix) {
        Node n=findNode(prefix,root);
        return collectKeys(prefix,n);
    }

    private Node findNode(String prefix, Node n){
        Character target=prefix.charAt(0);
        if (n.next.get(target.hashCode())==null) {
            throw new RuntimeException("we cannot do this!");
        }
        if (prefix.length()==1)
        {return n.next.get(target.hashCode());}
        else
        {return findNode(prefix.substring(1),n.next.get(target.hashCode()));}
    }



    public static void main(String[] args) {
        String[] saStrings = new String[]{"same", "am", "ad", "ap"};
        String[] otherStrings = new String[]{"a", "awls", "hallo"};

        MyTrieSet t = new MyTrieSet();
        for (String s : saStrings) {
            t.add(s);
        }
        for (String s : otherStrings) {
            t.add(s);
        }
        List<String> keys = t.keysWithPrefix("a");
        System.out.print(keys);
    }
}
