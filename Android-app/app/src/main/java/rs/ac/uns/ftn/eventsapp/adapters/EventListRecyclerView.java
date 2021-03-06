package rs.ac.uns.ftn.eventsapp.adapters;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import org.threeten.bp.ZonedDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import rs.ac.uns.ftn.eventsapp.R;
import rs.ac.uns.ftn.eventsapp.activities.EventDetailsActivity;
import rs.ac.uns.ftn.eventsapp.activities.UpdateEventActivity;
import rs.ac.uns.ftn.eventsapp.apiCalls.EventsAppAPI;
import rs.ac.uns.ftn.eventsapp.dtos.EventDTO;
import rs.ac.uns.ftn.eventsapp.utils.AppDataSingleton;
import rs.ac.uns.ftn.eventsapp.utils.ZonedGsonBuilder;

public class EventListRecyclerView extends RecyclerView.Adapter<EventListRecyclerView.EventViewHolder> {

    private List<EventDTO> mItems;
    private Context context;
    private int listRowResourceLayout;
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MMM yyyy HH:mm z");

    public EventListRecyclerView(List<EventDTO> items, Context context, @NonNull int listRowResourceLayout) {
        mItems = items;
        this.context = context;
        this.listRowResourceLayout = listRowResourceLayout;
    }

    @Override
    public EventViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        View v = LayoutInflater.from(viewGroup.getContext()).inflate(listRowResourceLayout, viewGroup, false);

        return new EventViewHolder(v);
    }

    @Override
    public void onBindViewHolder(EventViewHolder viewHolder, final int i) {
        EventDTO item = mItems.get(i);
        viewHolder.eventNameTextView.setText(item.getName());
        viewHolder.eventAddressTextView.setText(item.getPlace());
        String startDate = formatter.format(item.getStart_time());
        viewHolder.eventStartDate.setText(startDate.substring(0, startDate.length() - 6));
        String imageUri = item.getImageUri();

        if (imageUri != null && !imageUri.equals("") && !imageUri.startsWith("http")) {
            imageUri = AppDataSingleton.IMAGE_URI + imageUri;
        } else if (imageUri == null || imageUri.equals("")) {
            imageUri = "image"; //for picasso to not crash if image is empty or null
        }
        if(viewHolder.eventDistance!=null && viewHolder.eventDistanceImg!=null) {
            if (item.getDistance() != null) {
                viewHolder.eventDistance.setText(String.valueOf(item.getDistance()).substring(0, 6) + " km");
            } else {
                viewHolder.eventDistance.setVisibility(View.INVISIBLE);
                viewHolder.eventDistanceImg.setVisibility(View.INVISIBLE);
            }
        }

        Picasso.get()
                .load(imageUri)
                .placeholder(R.drawable.ic_missing_event_icon)
                .error(R.drawable.ic_missing_event_icon)
                .into(viewHolder.eventImage);


        //postavljanje listenera za going/interested/cancel dugmice
        setOnClickListeners(viewHolder, item);

        LinearLayout invitationBody = viewHolder.itemView.findViewById(R.id.event_row_body);
        invitationBody.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = view.getContext();
                Intent detailsIntent = new Intent(context, EventDetailsActivity.class);

                EventDTO e = mItems.get(i);
                String imageUri = e.getImageUri();
                if (imageUri != null && !imageUri.equals("") && !imageUri.startsWith("http")) {
                    e.setImageUri(AppDataSingleton.IMAGE_URI + imageUri);
                }

                detailsIntent.putExtra("EVENT", e);
                context.startActivity(detailsIntent);
            }
        });
    }

    /**
     * Setovanje onClickListenera za dugmeice (KOJI MOZDA NE POSTOJE NA SVAKOM LAYOUTU koji se ovde prosledi - zato je if null
     *
     * @param viewHolder
     */
    private void setOnClickListeners(@NonNull final EventViewHolder viewHolder, final EventDTO item) {
        ImageView imageInterestedAction =
                viewHolder.itemView.findViewById(R.id.image_interested_item_invitation);
        ImageView imageGoingAction =
                viewHolder.itemView.findViewById(R.id.image_goind_item_invitation);
        ImageView imageDeclineAction =
                viewHolder.itemView.findViewById(R.id.image_not_interested_item_invitation);
        ImageView deleteEvent =
                viewHolder.itemView.findViewById(R.id.deleteEvent);
        ImageView updateEvent =
                viewHolder.itemView.findViewById(R.id.updateEvent);
        final ImageView notInterested =
                viewHolder.itemView.findViewById(R.id.image_not_interested_item_invitation);
        ImageView notGoing =
                viewHolder.itemView.findViewById(R.id.image_not_going_item_invitation);

        if(item.getExpired()!=null){
            if(imageInterestedAction!= null && item.getExpired()){
                imageInterestedAction.setVisibility(View.INVISIBLE);
            }else if(imageInterestedAction!= null && !item.getExpired()){
                imageInterestedAction.setVisibility(View.VISIBLE);
            }
            if(imageGoingAction!= null && item.getExpired().equals(true)){
                imageGoingAction.setVisibility(View.INVISIBLE);
            }else if(imageGoingAction!= null && !item.getExpired()){
                imageGoingAction.setVisibility(View.VISIBLE);
            }
        }

        if (imageInterestedAction != null) {
            imageInterestedAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    interestedInEvent(viewHolder, item);
                }
            });
        }

        if (imageGoingAction != null) {
            imageGoingAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    goingToEvent(viewHolder, item);
                }
            });
        }

        if (imageDeclineAction != null) {
            imageDeclineAction.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Toast.makeText(viewHolder.itemView.getContext(), "A jbga, nista onda drugi put",
                            Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (deleteEvent != null) {
            deleteEvent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    deleteMyEvent(viewHolder, item);
                }
            });
        }

        if (updateEvent != null) {
            updateEvent.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(v.getContext(), UpdateEventActivity.class);
                    intent.putExtra("EventId", String.valueOf(item.getId()));
                    intent.putExtra("EventLat", Double.toString(item.getLatitude()));
                    intent.putExtra("EventLng", Double.toString(item.getLongitude()));
                    intent.putExtra("EventPlace", item.getPlace());
                    intent.putExtra("EventName", item.getName());
                    intent.putExtra("EventDes", item.getDescription());
                    intent.putExtra("EventStart", item.getStart_time().toString());
                    intent.putExtra("EventEnd", item.getEnd_time().toString());
                    intent.putExtra("EventCategory", item.getType());
                    intent.putExtra("EventPrivacy", item.getPrivacy());
                    intent.putExtra("EventImg", item.getImageUri());
                    v.getContext().startActivity(intent);
                }
            });
        }

        if (notInterested != null) {
            notInterested.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeFromInterested(viewHolder, item);
                }
            });
        }

        if (notGoing != null) {
            notGoing.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    removeFromGoing(viewHolder, item);
                }
            });
        }

    }

    @Override
    public int getItemCount() {
        return mItems == null ? 0 : mItems.size();
    }

    public static class EventViewHolder extends RecyclerView.ViewHolder {

        private final TextView eventNameTextView;
        private final TextView eventAddressTextView;
        private final TextView eventStartDate;
        private final TextView eventDistance;
        private final ImageView eventImage;
        private final ImageView eventDistanceImg;

        EventViewHolder(View v) {
            super(v);
            eventNameTextView = v.findViewById(R.id.event_list_name);
            eventAddressTextView = v.findViewById(R.id.event_list_address);
            eventStartDate = v.findViewById(R.id.event_list_start_date);
            eventImage = v.findViewById(R.id.event_list_image);
            eventDistance = v.findViewById(R.id.event_distance);
            eventDistanceImg = v.findViewById(R.id.event_distance_img);
        }
    }

    public void interestedInEvent(@NonNull final EventViewHolder viewHolder, final EventDTO item) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppDataSingleton.getInstance().SERVER_IP)
                .addConverterFactory(ZonedGsonBuilder.getZonedGsonFactory())
                .build();
        EventsAppAPI e = retrofit.create(EventsAppAPI.class);
        Call<EventDTO> s = e.interestedInEvent(item.getId(), AppDataSingleton.getInstance().getLoggedUser().getId());
        s.enqueue(new retrofit2.Callback<EventDTO>() {
            @Override
            public void onResponse(Call<EventDTO> call, Response<EventDTO> response) {
                if (response.code() != 200) {
                    Toast.makeText(viewHolder.itemView.getContext(), R.string.already_interested, Toast.LENGTH_LONG).show();
                } else {
                    Log.d("TAG", response.body().getId().toString());
                    mItems.remove(item);
                    notifyDataSetChanged();
                    AppDataSingleton.getInstance().setEventToInterested(item);
                    Toast.makeText(viewHolder.itemView.getContext(), R.string.added_to_inteerested, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<EventDTO> call, Throwable t) {
                Toast.makeText(viewHolder.itemView.getContext(), R.string.failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void goingToEvent(@NonNull final EventViewHolder viewHolder, final EventDTO event) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppDataSingleton.getInstance().SERVER_IP)
                .addConverterFactory(ZonedGsonBuilder.getZonedGsonFactory())
                .build();
        EventsAppAPI e = retrofit.create(EventsAppAPI.class);
        Call<EventDTO> s = e.goingToEvent(event.getId(), AppDataSingleton.getInstance().getLoggedUser().getId());
        s.enqueue(new retrofit2.Callback<EventDTO>() {
            @Override
            public void onResponse(Call<EventDTO> call, Response<EventDTO> response) {
                if (response.code() != 200) {
                    Toast.makeText(viewHolder.itemView.getContext(), R.string.already_going, Toast.LENGTH_LONG).show();
                } else {
                    Log.d("TAG", response.body().getId().toString());
                    mItems.remove(event);
                    notifyDataSetChanged();
                    AppDataSingleton.getInstance().setEventToGoing(event);
                    Toast.makeText(viewHolder.itemView.getContext(), R.string.added_to_going, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<EventDTO> call, Throwable t) {
                Toast.makeText(viewHolder.itemView.getContext(), R.string.failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void removeFromInterested(@NonNull final EventViewHolder viewHolder, final EventDTO event) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppDataSingleton.getInstance().SERVER_IP)
                .addConverterFactory(ZonedGsonBuilder.getZonedGsonFactory())
                .build();
        EventsAppAPI e = retrofit.create(EventsAppAPI.class);
        Call<EventDTO> s = e.removeInterestedEvent(event.getId(), AppDataSingleton.getInstance().getLoggedUser().getId());
        s.enqueue(new retrofit2.Callback<EventDTO>() {
            @Override
            public void onResponse(Call<EventDTO> call, Response<EventDTO> response) {
                if (response.code() != 200) {
                    Toast.makeText(viewHolder.itemView.getContext(), R.string.event_not_found, Toast.LENGTH_LONG).show();
                } else {
                    Log.d("TAG", response.body().getId().toString());
                    mItems.remove(event);
                    notifyDataSetChanged();
                    AppDataSingleton.getInstance().deleteInterestedEventPhysical(event.getId());
                    Toast.makeText(viewHolder.itemView.getContext(), R.string.removed_from_interested, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<EventDTO> call, Throwable t) {
                Toast.makeText(viewHolder.itemView.getContext(), R.string.failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void removeFromGoing(@NonNull final EventViewHolder viewHolder, final EventDTO event) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppDataSingleton.getInstance().SERVER_IP)
                .addConverterFactory(ZonedGsonBuilder.getZonedGsonFactory())
                .build();
        EventsAppAPI e = retrofit.create(EventsAppAPI.class);
        Call<EventDTO> s = e.removeGoingEvent(event.getId(), AppDataSingleton.getInstance().getLoggedUser().getId());
        s.enqueue(new retrofit2.Callback<EventDTO>() {
            @Override
            public void onResponse(Call<EventDTO> call, Response<EventDTO> response) {
                if (response.code() != 200) {
                    Toast.makeText(viewHolder.itemView.getContext(), R.string.event_not_found, Toast.LENGTH_LONG).show();
                } else {
                    Log.d("TAG", response.body().getId().toString());
                    mItems.remove(event);
                    notifyDataSetChanged();
                    AppDataSingleton.getInstance().deleteGoingEventPhysical(event.getId());
                    Toast.makeText(viewHolder.itemView.getContext(), R.string.removed_from_going, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<EventDTO> call, Throwable t) {
                Toast.makeText(viewHolder.itemView.getContext(), R.string.failed, Toast.LENGTH_LONG).show();
            }
        });
    }

    public void deleteMyEvent(@NonNull final EventViewHolder viewHolder, final EventDTO event) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(AppDataSingleton.getInstance().SERVER_IP)
                .addConverterFactory(ZonedGsonBuilder.getZonedGsonFactory())
                .build();
        EventsAppAPI e = retrofit.create(EventsAppAPI.class);
        Call<EventDTO> s = e.removeMyEvent(AppDataSingleton.getInstance().getLoggedUser().getId(), event.getId());
        s.enqueue(new retrofit2.Callback<EventDTO>() {
            @Override
            public void onResponse(Call<EventDTO> call, Response<EventDTO> response) {
                if (response.code() != 200) {
                    Toast.makeText(viewHolder.itemView.getContext(), R.string.event_not_found, Toast.LENGTH_LONG).show();
                } else {
                    Log.d("TAG", response.body().getId().toString());
                    mItems.remove(event);
                    notifyDataSetChanged();
                    Toast.makeText(viewHolder.itemView.getContext(), R.string.event_deleted, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<EventDTO> call, Throwable t) {
                Toast.makeText(viewHolder.itemView.getContext(), R.string.failed, Toast.LENGTH_LONG).show();
            }
        });
    }

}

