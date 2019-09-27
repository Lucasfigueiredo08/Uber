package com.uber.cursoandroid.jamiltondamasceno.uber.helper;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.uber.cursoandroid.jamiltondamasceno.uber.config.ConfiguracaoFirebase;

public class UsuarioFirebase {

    public static FirebaseUser getUsuarioAtual(){
        FirebaseAuth usuario = ConfiguracaoFirebase.getFirebaseAutenticacao();
        return usuario.getCurrentUser();
    }

    public static boolean atualizarNomeUsuario(String nome){
       try {
           FirebaseUser user = getUsuarioAtual();
           UserProfileChangeRequest profile =  new UserProfileChangeRequest.Builder()
                   .setDisplayName( nome )
                   .build();
           user.updateProfile( profile ).addOnCompleteListener(new OnCompleteListener<Void>() {
               @Override
               public void onComplete(@NonNull Task<Void> task) {
                    if(!task.isSuccessful()){
                        Log.d("Perfil", "Erro ao atualizar nome de perfil.");
                    }
               }
           });
           return true;
       } catch(Exception e){
           e.printStackTrace();
           return false;
       }
    }
    
}
