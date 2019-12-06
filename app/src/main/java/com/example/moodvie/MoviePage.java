package com.example.moodvie;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.databases.movies;
import com.example.objects.Person;
import com.iarcuschin.simpleratingbar.SimpleRatingBar;
import com.squareup.picasso.Picasso;

import java.util.Objects;

public class MoviePage extends AppCompatActivity
{
    // Return the super class of a views ID
    private <T extends View> T getView(int id) { return super.findViewById(id); }

    // Instantiate the movies database and functions class
    private final functions _functions = new functions();
    private final movies mdb = new movies(this);

    // Reference variable for a Person object
    private Person person;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.movie_page);

        // Get the bundle objects
        Bundle b = getIntent().getExtras();

        // If the bundles empty throw an AssertionError
        if (b == null)
            throw new AssertionError("Bundle cannot be empty");

        // Get the information from the bundle
        final String movieTitle = b.getString("movieTitle");
        final String movieOverview = b.getString("movieOverview");
        final String moviePoster = b.getString("moviePoster");
        final float movieRating = Float.parseFloat(b.getString("movieRating"));
        final String movieCast = b.getString("movieCast");
        final String movieGenres = b.getString("movieGenres");
        final String caller = b.getString("caller");

        // De-serialise the person object stored in the Intent extras
        person = (Person) getIntent().getSerializableExtra("personClass");

        // Create the toolbar
        Toolbar toolbar = getView(R.id.tvLogo);

        toolbar.setNavigationOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                finish();
            }
        });

        /*
         * As the movie page is used when a barcode has been scanned or when someone clicks a movie
         * on the home screen certain buttons need to be removed.
         *
         * If the caller of the MoviePage activity was the home screen then the the add movie button
         * has to be removed as they have already added that movie, this applies when the
         * caller of the activity is the barcode scanner also - the delete movie button will be
         * removed instead since it has not yet been added.
         *
         * Appropriate onClickListeners will be set up for the buttons remaining on the page
         */
        switch(Objects.requireNonNull(caller))
        {
            /*
             * If the caller is the MainHomeScreen activity then delete the add movie button from the
             * View and set up an OnClickListener to handle the button click of the delete movie
             * button
             */
            case "HomeScreen":
                // Remove the add movie button
                deleteButton((ViewGroup) getView(R.id.btnAddMovie).getParent(), R.id.btnAddMovie);

                // OnClickListener for the delete movie button
                getView(R.id.btnDeleteMovie).setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                         /*
                          * When the delete movie button is pressed create an alert dialog so that
                          * a user can confirm the deletion of the movie or cancel if this was
                          * pressed by accident
                          */
                        AlertDialog.Builder builder = new AlertDialog.Builder(MoviePage.this);
                        builder.setCancelable(true);
                        builder.setTitle(getString(R.string.alert_delete_movie, movieTitle));
                        builder.setMessage(getString(R.string.delete_movie_alert_message));

                        // Handles the click listener for the confirm option in the dialog
                        builder.setPositiveButton(getString(R.string.confirm_message), new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                /*
                                 * Try delete the movie out of the users stored movies and notify
                                 * them of the outcome
                                 */
                                if(mdb.deleteMovie(person.getUsername(), movieTitle))
                                {
                                    _functions.createMessage(getApplicationContext(),  getString(R.string.successfully_deleted_movie, movieTitle));
                                    finish();
                                }
                                else
                                {
                                    _functions.createMessage(getApplicationContext(), getString(R.string.failed_to_delete_movie, movieTitle));
                                    finish();
                                }

                            }
                        });

                        // Handles the click listener for the cancel option in the dialog
                        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                // do nothing
                            }
                        });

                        // Create and show the dialog
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });
                break;

            /*
             * If the caller is the BarcodeScanner activity then remove the delete movie button
             * from the View and set up an OnClickListener to handle the button click of the add
             * movie button
             */
            case "BarcodeScanner":
                // Remove the delete movie button
                deleteButton((ViewGroup) getView(R.id.btnDeleteMovie).getParent(), R.id.btnDeleteMovie);

                // OnClickListener for the add movie button
                ImageButton addMovie = getView(R.id.btnAddMovie);
                addMovie.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                         /*
                          * Check if the user already owns the movie they're trying to add;
                          * if they own it then notify them otherwise add the movie
                          */
                        if(mdb.movieExists(person.getUsername(), movieTitle))
                        {
                            _functions.createMessage(getApplicationContext(), getString(R.string.already_own_movie));
                            finish();
                        }
                        else
                        {
                             /*
                              * If they don't own the movie then try add it to their stored movies
                              * and notify them of the outcome
                              */
                            if(mdb.addData(movieTitle, movieOverview, movieCast, movieGenres, String.valueOf(movieRating), moviePoster, person.getUsername()))
                            {
                                _functions.createMessage(getApplicationContext(), getString(R.string.added_movie));
                                startActivity(new Intent(getApplicationContext(), HomeScreen.class).putExtra("personClass", person));
                                finish();
                            }
                            else
                            {
                                _functions.createMessage(getApplicationContext(), getString(R.string.failed_to_add_movie));
                                finish();
                            }
                        }
                    }
                });
                break;
        }


        // Grab the tvMovieTitle (TextView) and set the movie title
        final TextView title = getView(R.id.tvMovieTitle);
        title.setText(movieTitle);

        // Grab the tvMovieDesc (TextView) and set the movie synopsis
        final TextView overview = getView(R.id.tvMovieDesc);
        overview.setText(movieOverview);

        // Grab the ivMovePoster (ImageView) and set the movie poster
        ImageView movieImage = getView(R.id.ivMovePoster);
        Picasso.get().load(getString(R.string.image_path, moviePoster)).into(movieImage);

        // Grab the movieRatingBar (SimpleRatingBar) and animate it
        final SimpleRatingBar rating = getView(R.id.movieRatingBar);
        animateRatingBar(rating, Math.round(movieRating)/2f);

        // Grab the tvCast (TextView) and set the movie cast
        final TextView cast = getView(R.id.tvCast);
        cast.setText(movieCast);

    }

    /**
     * Removes a button from an activity if the ViewGroup of the button exists
     *
     * @param parent The ViewGroup of the button
     * @param view The ID of the button
     */
    private void deleteButton(ViewGroup parent, int view)
    {
        if(parent != null)
            parent.removeView(getView(view));
    }

    /**
     * Adds a 2 second animation to a SimpleRatingBar rating bar that wont repeat and
     * has a bounce effect
     *
     * @param ratingBar the SimpleRatingBar rating bar to thats to be animated
     * @param target the number of stars
     */
    private void animateRatingBar(SimpleRatingBar ratingBar, Float target)
    {
        SimpleRatingBar.AnimationBuilder builder = ratingBar.getAnimationBuilder()
                .setRatingTarget(target)
                .setDuration(2000)
                .setRepeatCount(0)
                .setInterpolator(new FastOutSlowInInterpolator());
        builder.start();
        ratingBar.setRating(target);
    }
}
