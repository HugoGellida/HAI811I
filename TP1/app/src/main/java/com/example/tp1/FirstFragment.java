package com.example.tp1;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.tp1.databinding.FragmentFirstBinding;

public class FirstFragment extends Fragment {

    private FragmentFirstBinding binding;
    private SharedViewModel viewModel;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentFirstBinding.inflate(inflater, container, false);
        viewModel = new ViewModelProvider(requireActivity())
                .get(SharedViewModel.class);

        binding.buttonFirst.setOnClickListener(v -> {
            String name = binding.name.getText().toString().trim();
            String age = binding.age.getText().toString().trim();
            String first_name = binding.firstName.getText().toString().trim();
            String comp = binding.comp.getText().toString().trim();
            String phone = binding.phone.getText().toString().trim();
            viewModel.setName(name);
            viewModel.setAge(Integer.parseInt(age));
            viewModel.setFirst_name(first_name);
            viewModel.setComp(comp);
            viewModel.setPhone(Integer.parseInt(phone));
            SecondFragment dialog = new SecondFragment();
            dialog.show(getParentFragmentManager(), "SecondDialog");
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.buttonFirst.setOnClickListener(v -> {
            SecondFragment dialog = new SecondFragment();
            dialog.show(getParentFragmentManager(), "SecondDialog");
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
