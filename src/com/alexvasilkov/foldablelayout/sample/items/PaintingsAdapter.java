package com.alexvasilkov.foldablelayout.sample.items;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.alexvasilkov.foldablelayout.sample1.R;
import com.alexvasilkov.foldablelayout.sample1.activities.UnfoldableDetailsActivity;
import com.azcltd.fluffycommons.adapters.ItemsAdapter;
import com.azcltd.fluffycommons.utils.Views;
import com.squareup.picasso.Picasso;

import java.util.Arrays;

public class PaintingsAdapter extends ItemsAdapter<Painting> implements View.OnClickListener {

    public PaintingsAdapter(Context context) {
        super(context);
        setItemsList(Arrays.asList(Painting.getAllPaintings(context.getResources())));
    }

    @Override
    protected View createView(Painting item, int pos, ViewGroup parent, LayoutInflater inflater) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false);
        ViewHolder vh = new ViewHolder();
        vh.image = Views.find(view, R.id.list_item_image);
        vh.image.setOnClickListener(this);
        vh.title = Views.find(view, R.id.list_item_title);
        view.setTag(vh);

        return view;
    }

    @Override
    protected void bindView(Painting item, int pos, View convertView) {
        ViewHolder vh = (ViewHolder) convertView.getTag();

        vh.image.setTag(item);
        Picasso.with(convertView.getContext()).load(item.getImageId()).into(vh.image);
//        vh.image.setImageResource(item.getImageId());
        vh.title.setText(item.getTitle());
    }

    @Override
    public void onClick(View view) {
        if (view.getContext() instanceof UnfoldableDetailsActivity) {
            UnfoldableDetailsActivity activity = (UnfoldableDetailsActivity) view.getContext();
            activity.openDetails(view, (Painting) view.getTag());
        }
    }

    private static class ViewHolder {
        ImageView image;
        TextView title;
    }

}
