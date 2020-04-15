package rs.ac.uns.ftn.eventsapp.fragments;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.xwray.groupie.GroupAdapter;
import com.xwray.groupie.GroupieViewHolder;

import rs.ac.uns.ftn.eventsapp.R;
import rs.ac.uns.ftn.eventsapp.views.InvitationItem;


public class InvitationsFragment extends Fragment {

    public InvitationsFragment() {
        // Required empty public constructor
    }

    public static InvitationsFragment newInstance(String param1, String param2) {
        InvitationsFragment fragment = new InvitationsFragment();
        Bundle args = new Bundle();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_invitations, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getAllInvitations();
    }

    private void getAllInvitations() {
        GroupAdapter<GroupieViewHolder> adapter = new GroupAdapter<GroupieViewHolder>();
        RecyclerView recyclerView =
                getActivity().findViewById(R.id.recyclerview_fragment_invitations);

        adapter.add(new InvitationItem());
        adapter.add(new InvitationItem());
        adapter.add(new InvitationItem());
        adapter.add(new InvitationItem());
        adapter.add(new InvitationItem());

        recyclerView.setAdapter(adapter);
    }
}
