package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.SecondaryEmerald
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    // Animation states
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(Unit) {
        transitionState.targetState = true
        // Keep splash screen for 1 second before transitioning
        delay(1000)
        onSplashFinished()
    }

    val transition = updateTransition(transitionState, label = "SplashTransition")

    val logoScale by transition.animateFloat(
        transitionSpec = {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
        },
        label = "LogoScale"
    ) { state -> if (state) 1f else 0.6f }

    val logoAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 800, easing = FastOutSlowInEasing) },
        label = "LogoAlpha"
    ) { state -> if (state) 1f else 0f }

    val bottomAlpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = 1000, delayMillis = 400, easing = FastOutSlowInEasing) },
        label = "BottomAlpha"
    ) { state -> if (state) 1f else 0f }

    // Pulsing effect for background glow
    val infiniteTransition = rememberInfiniteTransition(label = "PulseInfinite")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseGlow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8FAFC),
                        Color(0xFFEFF6FF),
                        Color(0xFFE2E8F0)
                    )
                )
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                // Allow tapping to skip splash
                onSplashFinished()
            }
    ) {
        // Decorative top glowing circle
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 60.dp, y = (-40).dp)
                .size(240.dp)
                .scale(pulseGlow)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.08f))
        )

        // Main Center Content: SCS360 Brand & Logo
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Main App Logo Container
            Surface(
                modifier = Modifier
                    .scale(logoScale)
                    .alpha(logoAlpha)
                    .fillMaxWidth()
                    .height(136.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFFF6F9F6),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.scs360_logo),
                        contentDescription = "SCS360 Resmi Logo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "DAHA AKILLI OKULLAR, DAHA GÜÇLÜ GELECEK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.1.sp,
                        color = Color(0xFF163B3B).copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "Yeni Nesil Kurumsal Okul Yönetim Platformu",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(logoAlpha)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Smooth linear progress indicator
            LinearProgressIndicator(
                color = PrimaryBlue,
                trackColor = PrimaryBlue.copy(alpha = 0.15f),
                modifier = Modifier
                    .width(120.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .alpha(logoAlpha)
            )
        }

        // Bottom Footer: ByteBridge Branding & Powered by
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
                .alpha(bottomAlpha),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "POWERED BY",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            Surface(
                color = Color.White,
                shape = RoundedCornerShape(20.dp),
                shadowElevation = 4.dp,
                tonalElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // ByteBridge Logo Image
                    Image(
                        painter = painterResource(id = R.drawable.img_bytebridge_logo_1787049913477),
                        contentDescription = "ByteBridge Logo",
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Column {
                        Text(
                            text = "ByteBridge",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandNavy,
                            lineHeight = 16.sp
                        )
                        Text(
                            text = "Technologies & Cloud SaaS",
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SecondaryEmerald,
                            lineHeight = 11.sp
                        )
                    }
                }
            }

            Text(
                text = "v1.0.0 • Tüm Hakları Saklıdır",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
