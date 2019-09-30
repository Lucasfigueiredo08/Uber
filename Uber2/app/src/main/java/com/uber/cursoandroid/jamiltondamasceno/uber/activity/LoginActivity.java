package com.uber.cursoandroid.jamiltondamasceno.uber.activity;

import android.support.annotation.NonNull;
import android.support.design.widget.TextInputEditText;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.uber.cursoandroid.jamiltondamasceno.uber.R;
import com.uber.cursoandroid.jamiltondamasceno.uber.config.ConfiguracaoFirebase;
import com.uber.cursoandroid.jamiltondamasceno.uber.helper.UsuarioFirebase;
import com.uber.cursoandroid.jamiltondamasceno.uber.model.Usuario;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText campoEmail, campoSenha;
    private FirebaseAuth autenticacao;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //Inicializar componentes
        campoEmail = findViewById(R.id.editLoginEmail);
        campoSenha = findViewById(R.id.editLoginSenha);
    }

    public void validarLoginUsuario(View view){
        String textEmail = campoEmail.getText().toString();
        String textoSenha = campoSenha.getText().toString();

        if(!textEmail.isEmpty()) { // verifica o email
            if(!textoSenha.isEmpty()) { // verifica a senha
                Usuario usuario = new Usuario();
                usuario.setEmail(textEmail);
                usuario.setSenha((textoSenha));

                logarUSuario( usuario );

            } else {
                Toast.makeText(LoginActivity.this,
                        "Preencha a senha!",
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(LoginActivity.this,
                    "Preencha o email!",
                    Toast.LENGTH_SHORT).show();
        }
    }

    public void logarUSuario (Usuario usuario) {
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.signInWithEmailAndPassword(
                usuario.getEmail(), usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){

                    //verificara o tipo de usuário logado
                    // motorista / passageiro
                    UsuarioFirebase.redirecionaUsuarioLogado(LoginActivity.this);


                }else{
                    String excecao = "";
                    try{
                        throw task.getException();
                    } catch ( FirebaseAuthWeakPasswordException e) {
                        excecao = "Digiee uma senha mais forte!";
                    } catch ( FirebaseAuthInvalidCredentialsException e) {
                        excecao = "Por favor, digite um e-mail válido!";
                    } catch ( FirebaseAuthUserCollisionException e) {
                        excecao = "Esta conta já foi cadastrada";
                    } catch ( Exception e) {
                        excecao = "Erro ao cadastrar usuário: " + e.getMessage();
                        e.printStackTrace();
                    }

                    Toast.makeText(LoginActivity.this,
                            excecao,
                            Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
