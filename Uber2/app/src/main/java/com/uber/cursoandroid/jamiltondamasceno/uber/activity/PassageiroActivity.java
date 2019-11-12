package com.uber.cursoandroid.jamiltondamasceno.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.uber.cursoandroid.jamiltondamasceno.uber.R;
import com.uber.cursoandroid.jamiltondamasceno.uber.config.ConfiguracaoFirebase;
import com.uber.cursoandroid.jamiltondamasceno.uber.helper.UsuarioFirebase;
import com.uber.cursoandroid.jamiltondamasceno.uber.model.Destino;
import com.uber.cursoandroid.jamiltondamasceno.uber.model.Requisicao;
import com.uber.cursoandroid.jamiltondamasceno.uber.model.Usuario;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PassageiroActivity extends AppCompatActivity implements OnMapReadyCallback {

    //Componentes
    private EditText editDestino;
    private LinearLayout linearLayoutDestino;
    private Button buttonChamarUber;

    private GoogleMap mMap;
    private FirebaseAuth autenticacao;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localPassageiro;
    private boolean uberChamado = false;
    private DatabaseReference firebaseRef;
    private Requisicao requisicao;

    public PassageiroActivity() {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passageiro);

        //configurações iniciais
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        iniciailizarComponentes();

        //adiciona listener para status da requisição
        verificaStatusRequisicao();
    }

    private void verificaStatusRequisicao(){

        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        final DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicoesPesquisa = requisicoes.orderByChild("passageiro/id")
            .equalTo(usuarioLogado.getId());

        requisicoesPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {

                List<Requisicao> lista = new ArrayList<>();
                for (DataSnapshot ds: dataSnapshot.getChildren()) {
                    lista.add(ds.getValue(Requisicao.class));
                }
                Collections.reverse(lista);
                requisicao = lista.get(0); //requisicao mais recente

                switch (requisicao.getStatus()){
                    case Requisicao.STATUS_AGUARDANDO:
                        linearLayoutDestino.setVisibility(View.GONE);
                        buttonChamarUber.setText("Cancelar Uber");
                        uberChamado = true;
                        break;
                }
            }
//            public void onDataChange(DataSnapshot dataSnapshot) {
//                for (DataSnapshot ds: dataSnapshot.getChildren()) {
//                    requisicao = ds.getValue(Requisicao.class);
////
////                    switch (){
////
////                    }
////                    break;
//                }
//            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        //Recupearar localizacao do usuario
        recuperarLocalizacaoUsuario();
    }

    public void chamarUber(View view){
        if(!uberChamado){ // uber n chamado
            String enderecoDestino = editDestino.getText().toString();
            if(!enderecoDestino.equals("") || enderecoDestino != null ){

                Address addressDestino = recuperarEndereco( enderecoDestino);
                if ( addressDestino != null ) {

                    final Destino destino = new Destino();
                    destino.setCidade(addressDestino.getAdminArea());
                    destino.setCep(addressDestino.getPostalCode());
                    destino.setBairro(addressDestino.getSubLocality());
                    destino.setRua(addressDestino.getThoroughfare());
                    destino.setNumero(addressDestino.getFeatureName());
                    destino.setLatitude(String.valueOf(addressDestino.getLatitude()));
                    destino.setLongitude(String.valueOf(addressDestino.getLongitude()));

                    StringBuilder mensagem = new StringBuilder();
                    mensagem.append("Cidade: " + destino.getCidade());
                    mensagem.append("\nRua: " + destino.getRua());
                    mensagem.append("\nBairro: " + destino.getBairro());
                    mensagem.append("\nNumero: " + destino.getNumero());
                    mensagem.append("\nCep: " + destino.getCep());

                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setTitle("Confirme seu endereco!")
                            .setMessage(mensagem)
                            .setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    //salvar requisição
                                    salvarRequisicao( destino );
                                    uberChamado = true;
                                }
                            }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

            }else {
                Toast.makeText(this,
                        "Informe o endereço de destino!",
                        Toast.LENGTH_SHORT).show();
            }
        }else{
            //cancelar requisição
            uberChamado = false;
        }


    }

    private void salvarRequisicao(Destino destino){

        Requisicao requisicao = new Requisicao();
        requisicao.setDestino(destino);

        Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioPassageiro.setLatitude(String.valueOf(localPassageiro.latitude));
        usuarioPassageiro.setLongitude(String.valueOf(localPassageiro.longitude));

        requisicao.setPassageiro( usuarioPassageiro );
        requisicao.setStatus(Requisicao.STATUS_AGUARDANDO);
        requisicao.salvar();

        linearLayoutDestino.setVisibility(View.GONE);
        buttonChamarUber.setText("Cancelar Uber");
    }

    private Address recuperarEndereco(String endereco){

        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> listaEnderecos = geocoder.getFromLocationName(endereco, 1);
            if (listaEnderecos != null && listaEnderecos.size() > 0){
                Address address = listaEnderecos.get(0);

                double lat = address.getLatitude();
                double lng = address.getLongitude();

                return address;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    private void recuperarLocalizacaoUsuario() {

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // recuperar lat e long
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localPassageiro = new LatLng(latitude, longitude);

                mMap.clear();

                mMap.addMarker(
                        new MarkerOptions()
                                .position(localPassageiro)
                                .title("Meu Local")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
                );
                mMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(localPassageiro, 20)
                );
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };

        //Solicitar atualizações de localização
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    locationListener
            );
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.menuSair:
                autenticacao.signOut();
                finish();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void iniciailizarComponentes(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle("Iniciar uma Viagem");
        setSupportActionBar(toolbar);

        //Inicializar Componentes
        editDestino = findViewById(R.id.editDestino);
        linearLayoutDestino = findViewById(R.id.linearLayoutDestino);
        buttonChamarUber = findViewById(R.id.idButtonChamarUber);

        //configuracoes iniciais
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }
}
