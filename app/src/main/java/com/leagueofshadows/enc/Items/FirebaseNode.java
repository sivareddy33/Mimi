package com.leagueofshadows.enc.Items;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;

import androidx.annotation.Nullable;

public class FirebaseNode {

    private String  node;
    private ChildEventListener childEventListener;
    private DatabaseReference databaseReference;

    public FirebaseNode(String node, ChildEventListener childEventListener,DatabaseReference databaseReference)
    {
        this.databaseReference = databaseReference;
        this.node = node;
        this.childEventListener = childEventListener;
    }

    public String getNode() {
        return node;
    }

    public void setNode(String node) {
        this.node = node;
    }

    public ChildEventListener getChildEventListener() {
        return childEventListener;
    }

    public void setChildEventListener(ChildEventListener childEventListener) {
        this.childEventListener = childEventListener;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj!=null && obj.getClass().equals(this.getClass())) {
            FirebaseNode f = (FirebaseNode) obj;
            return this.node.equals(f.getNode());
        }
        return false;
    }

    public DatabaseReference getDatabaseReference() {
        return databaseReference;
    }

    public void setDatabaseReference(DatabaseReference databaseReference) {
        this.databaseReference = databaseReference;
    }

    public void addChildEventListener()
    {
        if(childEventListener!=null)
            databaseReference.addChildEventListener(childEventListener);
    }
}
