package com.example.tp1;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class SharedViewModel extends ViewModel {
    private final MutableLiveData<String> name = new MutableLiveData<>();
    private final MutableLiveData<Integer> age = new MutableLiveData<>();
    private final MutableLiveData<String> first_name = new MutableLiveData<>();
    private final MutableLiveData<String> comp = new MutableLiveData<>();
    private final MutableLiveData<Integer> phone = new MutableLiveData<>();

    public void setName(String value) {
        name.setValue(value);
    }
    public LiveData<String> getName() {
        return name;
    }
    public void setAge(int value) {
        age.setValue(value);
    }
    public LiveData<Integer> getAge() {
        return age;
    }
    public MutableLiveData<String> getFirst_name() {
        return first_name;
    }

    public void setFirst_name(String value){
        first_name.setValue(value);
    }

    public MutableLiveData<String> getComp() {
        return comp;
    }

    public void setComp(String value){
        comp.setValue(value);
    }

    public MutableLiveData<Integer> getPhone() {
        return phone;
    }

    public void setPhone(Integer value){
        phone.setValue(value);
    }
}
